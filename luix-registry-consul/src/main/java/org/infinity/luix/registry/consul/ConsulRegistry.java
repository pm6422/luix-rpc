package org.infinity.luix.registry.consul;

import com.ecwid.consul.v1.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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

import static org.infinity.luix.core.constant.RegistryConstants.DISCOVERY_INTERVAL;
import static org.infinity.luix.core.constant.RegistryConstants.DISCOVERY_INTERVAL_VAL_DEFAULT;
import static org.infinity.luix.registry.consul.utils.ConsulUtils.CONSUL_PROVIDING_SERVICES_PREFIX;

@Slf4j
@ThreadSafe
public class ConsulRegistry extends CommandFailbackAbstractRegistry implements Destroyable {

    /**
     * Consul client
     */
    private final LuixConsulClient                                                    consulClient;
    /**
     * Consul service instance status updater
     */
    private final ConsulStatusUpdater                                                 consulStatusUpdater;
    /**
     * Service discovery interval in milliseconds
     */
    private final int                                                                 discoverInterval;
    /**
     * Provider service notification thread pool
     */
    private final ThreadPoolExecutor                                                  notificationThreadPool;
    /**
     * Cache used to store provider urls
     * Key:  form
     * Value: protocol plus path to urls map
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, List<Url>>>     urlCache         = new ConcurrentHashMap<>();
    /**
     * Cache used to store commands
     * Key: form
     * Value: command string
     */
    private final ConcurrentHashMap<String, String>                                   commandCache     = new ConcurrentHashMap<>();
    /**
     * Key: form
     * Value: lastConsulIndexId
     */
    private final ConcurrentHashMap<String, Long>                                     form2ConsulIndex = new ConcurrentHashMap<>();
    /**
     * Key: form
     * Value: command string
     */
    private final ConcurrentHashMap<String, String>                                   form2Command     = new ConcurrentHashMap<>();
    /**
     * Key: protocol plus path
     * Value: url to providerListener map
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Url, ProviderListener>> serviceListeners = new ConcurrentHashMap<>();
    /**
     * Key: protocol plus path
     * Value: url to commandListener map
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Url, CommandListener>>  commandListeners = new ConcurrentHashMap<>();

    public ConsulRegistry(Url registryUrl, LuixConsulClient consulClient) {
        super(registryUrl);
        this.consulClient = consulClient;
        consulStatusUpdater = new ConsulStatusUpdater(consulClient);
        consulStatusUpdater.start();
        discoverInterval = this.registryUrl.getIntOption(DISCOVERY_INTERVAL, DISCOVERY_INTERVAL_VAL_DEFAULT);
        notificationThreadPool = createNotificationThreadPool();
        ShutdownHook.add(this);
        log.info("Initialized consul registry");
    }

    private ThreadPoolExecutor createNotificationThreadPool() {
        return new ThreadPoolExecutor(10, 30, 30 * 1_000,
                TimeUnit.MILLISECONDS, createWorkQueue(), new ThreadPoolExecutor.AbortPolicy());
    }

    private BlockingQueue<Runnable> createWorkQueue() {
        return new ArrayBlockingQueue<>(20_000);
    }

    @Override
    protected void doRegister(Url providerUrl) {
        ConsulService service = ConsulService.byUrl(providerUrl);
        consulClient.registerService(service);
    }

    @Override
    protected void doDeregister(Url providerUrl) {
        ConsulService service = ConsulService.byUrl(providerUrl);
        consulClient.deregisterService(service.getInstanceId());
    }

    @Override
    protected void doActivate(Url providerUrl) {
        if (providerUrl == null) {
            // Activate all service instances
            consulStatusUpdater.updateStatus(true);
        } else {
            // Activate specified service instance
            consulStatusUpdater.activate(ConsulUtils.buildServiceInstanceId(providerUrl));
        }
    }

    @Override
    protected void doDeactivate(Url providerUrl) {
        if (providerUrl == null) {
            // Deactivate all service instances
            consulStatusUpdater.updateStatus(false);
        } else {
            // Deactivate specified service instance
            consulStatusUpdater.deactivate(ConsulUtils.buildServiceInstanceId(providerUrl));
        }
    }

    @Override
    public void subscribe(Url consumerUrl) {
        ConsulService service = ConsulService.byUrl(consumerUrl);
        consulClient.registerService(service);
        // Activate specified service instance
        consulStatusUpdater.activate(service.getInstanceId());
    }

    @Override
    public void unsubscribe(Url consumerUrl) {
        ConsulService service = ConsulService.byUrl(consumerUrl);
        consulClient.deregisterService(service.getInstanceId());
    }

    @Override
    protected List<Url> discoverActiveProviders(Url consumerUrl) {
        String protocolPlusPath = ConsulUtils.getProtocolPlusPath(consumerUrl);
        String form = consumerUrl.getForm();
        List<Url> providerUrls = new ArrayList<>();
        ConcurrentHashMap<String, List<Url>> protocolPlusPath2Urls = urlCache.get(form);
        if (protocolPlusPath2Urls == null) {
            synchronized (form.intern()) {
                protocolPlusPath2Urls = urlCache.get(form);
                if (protocolPlusPath2Urls == null) {
                    ConcurrentHashMap<String, List<Url>> protocolPlusPath2UrlsMap = doDiscoverActiveProviders(form);
                    updateProviderUrlsCache(form, protocolPlusPath2UrlsMap, false);
                    protocolPlusPath2Urls = urlCache.get(form);
                }
            }
        }
        if (protocolPlusPath2Urls != null) {
            providerUrls = protocolPlusPath2Urls.get(protocolPlusPath);
        }
        return providerUrls;
    }

    private ConcurrentHashMap<String, List<Url>> doDiscoverActiveProviders(String form) {
        ConcurrentHashMap<String, List<Url>> protocolPlusPath2Urls = new ConcurrentHashMap<>();
        Long lastConsulIndexId = form2ConsulIndex.get(form) == null ? 0L : form2ConsulIndex.get(form);
        Response<List<ConsulService>> response = consulClient
                .queryActiveServiceInstances(CONSUL_PROVIDING_SERVICES_PREFIX, lastConsulIndexId);
        if (response != null) {
            List<ConsulService> activeServiceInstances = response.getValue();
            if (CollectionUtils.isNotEmpty(activeServiceInstances) && response.getConsulIndex() > lastConsulIndexId) {
                for (ConsulService activeServiceInstance : activeServiceInstances) {
                    try {
                        Url url = ConsulUtils.buildUrl(activeServiceInstance);
                        String protocolPlusPath = ConsulUtils.getProtocolPlusPath(url);
                        List<Url> urls = protocolPlusPath2Urls.computeIfAbsent(protocolPlusPath, k -> new ArrayList<>());
                        urls.add(url);
                    } catch (Exception e) {
                        log.error("Failed to build url from consul service instance: " + activeServiceInstance, e);
                    }
                }
                form2ConsulIndex.put(form, response.getConsulIndex());
                return protocolPlusPath2Urls;
            } else {
                log.info("No active service found for form: [{}]", form);
            }
        }
        return protocolPlusPath2Urls;
    }

    private void updateProviderUrlsCache(String form, ConcurrentHashMap<String, List<Url>> protocolPlusPath2Urls, boolean needNotify) {
        if (MapUtils.isNotEmpty(protocolPlusPath2Urls)) {
            ConcurrentHashMap<String, List<Url>> protocolPlusPath2UrlsCopy = urlCache.putIfAbsent(form, protocolPlusPath2Urls);
            for (Map.Entry<String, List<Url>> entry : protocolPlusPath2Urls.entrySet()) {
                boolean change = true;
                if (protocolPlusPath2UrlsCopy != null) {
                    List<Url> oldUrls = protocolPlusPath2UrlsCopy.get(entry.getKey());
                    List<Url> newUrls = entry.getValue();
                    if (CollectionUtils.isEmpty(newUrls) || ConsulUtils.isSame(entry.getValue(), oldUrls)) {
                        change = false;
                    } else {
                        protocolPlusPath2UrlsCopy.put(entry.getKey(), newUrls);
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

    @Override
    protected void subscribeProviderListener(Url consumerUrl, ProviderListener listener) {
        addServiceListener(consumerUrl, listener);
        startListenerThreadIfNewService(consumerUrl);
    }

    private void addServiceListener(Url url, ProviderListener providerListener) {
        String protocolPlusPath = ConsulUtils.getProtocolPlusPath(url);
        ConcurrentHashMap<Url, ProviderListener> map = serviceListeners.get(protocolPlusPath);
        if (map == null) {
            serviceListeners.putIfAbsent(protocolPlusPath, new ConcurrentHashMap<>());
            map = serviceListeners.get(protocolPlusPath);
        }
        synchronized (map) {
            map.put(url, providerListener);
        }
    }

    private void startListenerThreadIfNewService(Url url) {
        if (!form2ConsulIndex.containsKey(url.getForm())) {
            Long consulIndex = form2ConsulIndex.putIfAbsent(url.getForm(), 0L);
            if (consulIndex == null) {
                DiscoverProviderThread discoverProviderThread = new DiscoverProviderThread(url.getForm());
                discoverProviderThread.setDaemon(true);
                discoverProviderThread.start();
            }
        }
    }

    @Override
    protected void unsubscribeProviderListener(Url consumerUrl, ProviderListener listener) {
        ConcurrentHashMap<Url, ProviderListener> url2ProviderListeners =
                serviceListeners.get(ConsulUtils.getProtocolPlusPath(consumerUrl));
        if (url2ProviderListeners != null) {
            synchronized (url2ProviderListeners) {
                url2ProviderListeners.remove(consumerUrl);
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
        if (!form2Command.containsKey(group)) {
            String command = form2Command.putIfAbsent(group, StringUtils.EMPTY);
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
    public List<Url> getAllProviderUrls() {
        return consulClient.getAllProviderUrls();
    }

    @Override
    public void subscribeConsumerListener(String interfaceName, ConsumerProcessable consumerProcessor) {
        List<Url> consumerUrls = consulClient.getConsumerUrls(interfaceName);
        consumerProcessor.process(getRegistryUrl(), interfaceName, consumerUrls);
    }

    @Override
    protected String readCommand(Url consumerUrl) {
        String group = consumerUrl.getForm();
        String command = lookupCommandUpdate(group);
        updateCommandCache(group, command, false);
        return command;
    }

    private String lookupCommandUpdate(String group) {
        String command = consulClient.queryCommand(group);
        form2Command.put(group, command);
        return command;
    }

    /**
     * update command cache of the group.
     * update local cache when command changed,
     * if need notify, notify command
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
        private final String form;
        private final String command;

        public NotifyCommand(String form, String command) {
            this.form = form;
            this.command = command;
        }

        @Override
        public void run() {
            ConcurrentHashMap<Url, CommandListener> listeners = commandListeners.get(form);
            synchronized (listeners) {
                for (Map.Entry<Url, CommandListener> entry : listeners.entrySet()) {
                    CommandListener commandListener = entry.getValue();
                    commandListener.onNotify(entry.getKey(), command);
                }
            }
        }
    }

    private class DiscoverProviderThread extends Thread {
        private final String form;

        public DiscoverProviderThread(String form) {
            this.form = form;
        }

        @Override
        public void run() {
            log.info("Start discover providers thread with interval: " + discoverInterval + "ms and form: " + form);
            while (true) {
                try {
                    sleep(discoverInterval);
                    ConcurrentHashMap<String, List<Url>> protocolPlusPath2Urls = doDiscoverActiveProviders(form);
                    updateProviderUrlsCache(form, protocolPlusPath2Urls, true);
                } catch (Throwable e) {
                    log.error("Failed to discover providers!", e);
                    try {
                        Thread.sleep(2_000);
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
            log.info("start command lookup thread. lookup interval: " + discoverInterval + "ms, group: " + group);
            while (true) {
                try {
                    sleep(discoverInterval);
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
        consulStatusUpdater.close();
        log.info("Destroyed consul registry");
    }
}
