package org.infinity.luix.registry.consul;

import com.ecwid.consul.v1.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.infinity.luix.core.registry.CommandFailbackAbstractRegistry;
import org.infinity.luix.core.registry.listener.CommandListener;
import org.infinity.luix.core.registry.listener.ProviderListener;
import org.infinity.luix.core.server.listener.ConsumerProcessable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.registry.consul.utils.ConsulUtils;
import org.infinity.luix.utilities.destory.Destroyable;
import org.infinity.luix.utilities.destory.ShutdownHook;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.infinity.luix.registry.consul.ConsulServiceStatusUpdater.STATUS_FAILING;
import static org.infinity.luix.registry.consul.ConsulServiceStatusUpdater.STATUS_PASSING;

@Slf4j
@ThreadSafe
public class ConsulRegistry extends CommandFailbackAbstractRegistry implements Destroyable {

    /**
     * consul服务查询默认间隔时间。单位毫秒
     */
    public static int                                                                 DEFAULT_LOOKUP_INTERVAL = 30_000;
    private final LuixConsulClient                                                    consulClient;
    private final ConsulServiceStatusUpdater                                          consulServiceStatusUpdater;
    private final int                                                                 lookupInterval;
    // service local cache. key: group, value: <service interface name, url list>
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, List<Url>>>     serviceCache            = new ConcurrentHashMap<>();
    // command local cache. key: group, value: command content
    private final ConcurrentHashMap<String, String>                                   commandCache            = new ConcurrentHashMap<>();
    // record lookup service thread, insure each group start only one thread, <group, lastConsulIndexId>
    private final ConcurrentHashMap<String, Long>                                     lookupGroupServices     = new ConcurrentHashMap<>();
    // record lookup command thread, <group, command>
    // TODO: 2016/6/17 change value to consul index
    private final ConcurrentHashMap<String, String>                                   lookupGroupCommands     = new ConcurrentHashMap<>();
    // TODO: 2016/6/17 clientUrl support multiple listener
    // record subscribers service callback listeners, listener was called when corresponding service changes
    private final ConcurrentHashMap<String, ConcurrentHashMap<Url, ProviderListener>> serviceListeners        = new ConcurrentHashMap<>();
    // record subscribers command callback listeners, listener was called when corresponding command changes
    private final ConcurrentHashMap<String, ConcurrentHashMap<Url, CommandListener>>  commandListeners        = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor                                                  notificationThreadPool;

    public ConsulRegistry(Url url, LuixConsulClient consulClient) {
        super(url);
        this.consulClient = consulClient;
        consulServiceStatusUpdater = new ConsulServiceStatusUpdater(consulClient);
        consulServiceStatusUpdater.start();
//        lookupInterval = super.registryUrl.getIntOption(URLParamType.registrySessionTimeout.getName(), DEFAULT_LOOKUP_INTERVAL);
        lookupInterval = DEFAULT_LOOKUP_INTERVAL;
        notificationThreadPool = createNotificationThreadPool();
        ShutdownHook.add(this);
        log.info("Initialized consul registry");
    }

    private ThreadPoolExecutor createNotificationThreadPool() {
        return new ThreadPoolExecutor(10, 30, 30 * 1_000,
                TimeUnit.MILLISECONDS, createWorkQueue());
    }

    private BlockingQueue<Runnable> createWorkQueue() {
        return new ArrayBlockingQueue<>(20_000);
    }

    @Override
    protected void doRegister(Url providerUrl) {
        ConsulService service = ConsulService.of(providerUrl);
        consulClient.registerService(service);
    }

    @Override
    protected void doDeregister(Url url) {
        ConsulService service = ConsulService.of(url);
        consulClient.deregisterService(service.getInstanceId());
    }

    @Override
    protected void doActivate(Url url) {
        if (url == null) {
            // Activate all service instances
            consulServiceStatusUpdater.updateStatus(STATUS_PASSING);
        } else {
            // Activate specified service instance
            consulServiceStatusUpdater.activate(ConsulUtils.buildServiceInstanceId(url));
        }
    }

    @Override
    protected void doDeactivate(Url url) {
        if (url == null) {
            // Deactivate all service instances
            consulServiceStatusUpdater.updateStatus(STATUS_FAILING);
        } else {
            // Deactivate specified service instance
            consulServiceStatusUpdater.deactivate(ConsulUtils.buildServiceInstanceId(url));
        }
    }

    @Override
    protected List<Url> discoverActiveProviders(Url consumerUrl) {
        String service = ConsulUtils.getProtocolPlusPath(consumerUrl);
        String form = consumerUrl.getForm();
        List<Url> serviceUrls = new ArrayList<>();
        ConcurrentHashMap<String, List<Url>> serviceMap = serviceCache.get(form);
        if (serviceMap == null) {
            synchronized (form.intern()) {
                serviceMap = serviceCache.get(form);
                if (serviceMap == null) {
                    ConcurrentHashMap<String, List<Url>> groupUrls = lookupServiceUpdate(form);
                    updateServiceCache(form, groupUrls, false);
                    serviceMap = serviceCache.get(form);
                }
            }
        }
        if (serviceMap != null) {
            serviceUrls = serviceMap.get(service);
        }
        return serviceUrls;
    }

    private ConcurrentHashMap<String, List<Url>> lookupServiceUpdate(String form) {
        ConcurrentHashMap<String, List<Url>> groupUrls = new ConcurrentHashMap<>();
        Long lastConsulIndexId = lookupGroupServices.get(form) == null ? 0L : lookupGroupServices.get(form);
        Response<List<ConsulService>> response = queryActiveServiceInstances(form, lastConsulIndexId);
        if (response != null) {
            List<ConsulService> services = response.getValue();
            if (CollectionUtils.isNotEmpty(services) && response.getConsulIndex() > lastConsulIndexId) {
                for (ConsulService service : services) {
                    try {
                        Url url = ConsulUtils.buildUrl(service);
                        String cluster = ConsulUtils.getProtocolPlusPath(url);
                        List<Url> urlList = groupUrls.computeIfAbsent(cluster, k -> new ArrayList<>());
                        urlList.add(url);
                    } catch (Exception e) {
                        log.error("convert consul service to url fail! service:" + service, e);
                    }
                }
                lookupGroupServices.put(form, response.getConsulIndex());
                return groupUrls;
            } else {
                log.info(form + " no need update, lastIndex:" + lastConsulIndexId);
            }
        }
        return groupUrls;
    }

    /**
     * update service cache of the group.
     * update local cache when service list changed,
     * if need notify, notify service
     *
     * @param form
     * @param groupUrls
     * @param needNotify
     */
    private void updateServiceCache(String form, ConcurrentHashMap<String, List<Url>> groupUrls, boolean needNotify) {
        if (MapUtils.isNotEmpty(groupUrls)) {
            ConcurrentHashMap<String, List<Url>> groupMap = serviceCache.putIfAbsent(form, groupUrls);
            for (Map.Entry<String, List<Url>> entry : groupUrls.entrySet()) {
                boolean change = true;
                if (groupMap != null) {
                    List<Url> oldUrls = groupMap.get(entry.getKey());
                    List<Url> newUrls = entry.getValue();
                    if (CollectionUtils.isEmpty(newUrls) || ConsulUtils.isSame(entry.getValue(), oldUrls)) {
                        change = false;
                    } else {
                        groupMap.put(entry.getKey(), newUrls);
                    }
                }
                if (change && needNotify) {
                    notificationThreadPool.execute(new NotifyService(entry.getKey(), entry.getValue()));
                    log.info("service notify-service: " + entry.getKey());
                    StringBuilder sb = new StringBuilder();
                    for (Url url : entry.getValue()) {
                        sb.append(url.getUri()).append(";");
                    }
                    log.info("consul notify urls:" + sb);
                }
            }
        }
    }

    private Response<List<ConsulService>> queryActiveServiceInstances(String serviceName, Long lastConsulIndexId) {
        return consulClient.queryActiveServiceInstances(ConsulUtils.buildServiceName(serviceName), lastConsulIndexId);
    }

    @Override
    protected void subscribeProviderListener(Url consumerUrl, ProviderListener listener) {
        addServiceListener(consumerUrl, listener);
        startListenerThreadIfNewService(consumerUrl);
    }

    private void addServiceListener(Url url, ProviderListener serviceListener) {
        String service = ConsulUtils.getProtocolPlusPath(url);
        ConcurrentHashMap<Url, ProviderListener> map = serviceListeners.get(service);
        if (map == null) {
            serviceListeners.putIfAbsent(service, new ConcurrentHashMap<>());
            map = serviceListeners.get(service);
        }
        synchronized (map) {
            map.put(url, serviceListener);
        }
    }

    /**
     * if new group registered, start a new lookup thread
     * each group start a lookup thread to discover service
     *
     * @param url
     */
    private void startListenerThreadIfNewService(Url url) {
        String group = url.getForm();
        if (!lookupGroupServices.containsKey(group)) {
            Long value = lookupGroupServices.putIfAbsent(group, 0L);
            if (value == null) {
                ServiceLookupThread lookupThread = new ServiceLookupThread(group);
                lookupThread.setDaemon(true);
                lookupThread.start();
            }
        }
    }

    @Override
    protected void unsubscribeProviderListener(Url consumerUrl, ProviderListener listener) {
        ConcurrentHashMap<Url, ProviderListener> listeners = serviceListeners.get(ConsulUtils.getProtocolPlusPath(consumerUrl));
        if (listeners != null) {
            synchronized (listeners) {
                listeners.remove(consumerUrl);
            }
        }
    }

    @Override
    protected void subscribeCommandListener(Url consumerUrl, CommandListener listener) {
        addCommandListener(consumerUrl, listener);
        startListenerThreadIfNewCommand(consumerUrl);
    }

    private void addCommandListener(Url url, CommandListener commandListener) {
        String group = url.getForm();
        ConcurrentHashMap<Url, CommandListener> map = commandListeners.get(group);
        if (map == null) {
            commandListeners.putIfAbsent(group, new ConcurrentHashMap<>());
            map = commandListeners.get(group);
        }
        synchronized (map) {
            map.put(url, commandListener);
        }
    }

    private void startListenerThreadIfNewCommand(Url url) {
        String group = url.getForm();
        if (!lookupGroupCommands.containsKey(group)) {
            String command = lookupGroupCommands.putIfAbsent(group, "");
            if (command == null) {
                CommandLookupThread lookupThread = new CommandLookupThread(group);
                lookupThread.setDaemon(true);
                lookupThread.start();
            }
        }
    }

    @Override
    protected void unsubscribeCommandListener(Url consumerUrl, CommandListener listener) {
        ConcurrentHashMap<Url, CommandListener> listeners = commandListeners.get(consumerUrl.getForm());
        if (listeners != null) {
            synchronized (listeners) {
                listeners.remove(consumerUrl);
            }
        }
    }

    @Override
    protected String readCommand(Url consumerUrl) {
        String group = consumerUrl.getForm();
        String command = lookupCommandUpdate(group);
        updateCommandCache(group, command, false);
        return command;
    }

    @Override
    public List<String> getAllProviderPaths() {
        return null;
    }

    @Override
    public List<String> discoverActiveProviderAddress(String providerPath) {
        return null;
    }

    @Override
    public void subscribeConsumerListener(String interfaceName, ConsumerProcessable consumerProcessor) {

    }

    private String lookupCommandUpdate(String group) {
        String command = consulClient.queryCommand(group);
        lookupGroupCommands.put(group, command);
        return command;
    }

    /**
     * update command cache of the group.
     * update local cache when command changed,
     * if need notify, notify command
     *
     * @param group
     * @param command
     * @param needNotify
     */
    private void updateCommandCache(String group, String command, boolean needNotify) {
        String oldCommand = commandCache.get(group);
        if (!command.equals(oldCommand)) {
            commandCache.put(group, command);
            if (needNotify) {
                notificationThreadPool.execute(new NotifyCommand(group, command));
                log.info(String.format("command data change: group=%s, command=%s: ", group, command));
            }
        } else {
            log.info(String.format("command data not change: group=%s, command=%s: ", group, command));
        }
    }

    protected Url getUrl() {
        return super.registryUrl;
    }

    private class NotifyService implements Runnable {
        private final String    service;
        private final List<Url> urls;

        public NotifyService(String service, List<Url> urls) {
            this.service = service;
            this.urls = urls;
        }

        @Override
        public void run() {
            ConcurrentHashMap<Url, ProviderListener> listeners = serviceListeners.get(service);
            if (listeners != null) {
                synchronized (listeners) {
                    for (Map.Entry<Url, ProviderListener> entry : listeners.entrySet()) {
                        ProviderListener serviceListener = entry.getValue();
                        serviceListener.onNotify(entry.getKey(), getUrl(), urls);
                    }
                }
            } else {
                log.debug("need not notify service:" + service);
            }
        }
    }

    private class NotifyCommand implements Runnable {
        private final String group;
        private final String command;

        public NotifyCommand(String group, String command) {
            this.group = group;
            this.command = command;
        }

        @Override
        public void run() {
            ConcurrentHashMap<Url, CommandListener> listeners = commandListeners.get(group);
            synchronized (listeners) {
                for (Map.Entry<Url, CommandListener> entry : listeners.entrySet()) {
                    CommandListener commandListener = entry.getValue();
                    commandListener.onNotify(entry.getKey(), command);
                }
            }
        }
    }

    private class ServiceLookupThread extends Thread {
        private final String group;

        public ServiceLookupThread(String group) {
            this.group = group;
        }

        @Override
        public void run() {
            log.info("start group lookup thread. lookup interval: " + lookupInterval + "ms, group: " + group);
            while (true) {
                try {
                    sleep(lookupInterval);
                    ConcurrentHashMap<String, List<Url>> groupUrls = lookupServiceUpdate(group);
                    updateServiceCache(group, groupUrls, true);
                } catch (Throwable e) {
                    log.error("group lookup thread fail!", e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    private class CommandLookupThread extends Thread {
        private final String group;

        public CommandLookupThread(String group) {
            this.group = group;
        }

        @Override
        public void run() {
            log.info("start command lookup thread. lookup interval: " + lookupInterval + "ms, group: " + group);
            while (true) {
                try {
                    sleep(lookupInterval);
                    String command = lookupCommandUpdate(group);
                    updateCommandCache(group, command, true);
                } catch (Throwable e) {
                    log.error("group lookup thread fail!", e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    @Override
    public void destroy() {
        notificationThreadPool.shutdown();
        consulServiceStatusUpdater.close();
        log.info("Destroyed consul registry");
    }
}
