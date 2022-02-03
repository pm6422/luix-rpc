package org.infinity.luix.registry.consul;

import com.ecwid.consul.v1.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.infinity.luix.core.registry.AbstractRegistry;
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
import static org.infinity.luix.registry.consul.utils.ConsulUtils.buildProviderServiceName;

@Slf4j
@ThreadSafe
public class ConsulRegistry extends AbstractRegistry implements Destroyable {

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
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, List<Url>>>     providerUrlCache           = new ConcurrentHashMap<>();
    /**
     * Key: form
     * Value: lastConsulIndexId
     */
    private final ConcurrentHashMap<String, Long>                                     form2ConsulIndex           = new ConcurrentHashMap<>();
    /**
     * Key: protocol plus path
     * Value: url to providerListener map
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Url, ProviderListener>> serviceListeners           = new ConcurrentHashMap<>();
    /**
     * Key: consumer path
     * Value: consumerUrls
     */
    private final ConcurrentHashMap<String, List<Url>>                                path2ConsumerUrls          = new ConcurrentHashMap<>();
    /**
     *
     */
    private final ScheduledExecutorService                                            consumerChangesMonitorPool = Executors.newSingleThreadScheduledExecutor();

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
    protected void doRegister(Url url) {
        ConsulService service = ConsulService.byUrl(url);
        consulClient.registerService(service);
    }

    @Override
    protected void doDeregister(Url url) {
        ConsulService service = ConsulService.byUrl(url);
        consulClient.deregisterService(service.getInstanceId());
    }

    @Override
    protected void doActivate(Url url) {
        if (url == null) {
            // Activate all service instances
            consulStatusUpdater.updateStatus(true);
        } else {
            // Activate specified service instance
            consulStatusUpdater.activate(ConsulUtils.buildServiceInstanceId(url));
        }
    }

    @Override
    protected void doDeactivate(Url url) {
        if (url == null) {
            // Deactivate all service instances
            consulStatusUpdater.updateStatus(false);
        } else {
            // Deactivate specified service instance
            consulStatusUpdater.deactivate(ConsulUtils.buildServiceInstanceId(url));
        }
    }

    @Override
    protected List<Url> discoverActiveProviders(Url consumerUrl) {
        String protocolPlusPath = ConsulUtils.getProtocolPlusPath(consumerUrl);
        String form = consumerUrl.getForm();
        List<Url> providerUrls = new ArrayList<>();
        ConcurrentHashMap<String, List<Url>> protocolPlusPath2Urls = providerUrlCache.get(form);
        if (protocolPlusPath2Urls == null) {
            synchronized (form.intern()) {
                protocolPlusPath2Urls = providerUrlCache.get(form);
                if (protocolPlusPath2Urls == null) {
                    ConcurrentHashMap<String, List<Url>> protocolPlusPath2UrlsMap = doDiscoverActiveProviders(form);
                    updateProviderUrlsCache(form, protocolPlusPath2UrlsMap, false);
                    protocolPlusPath2Urls = providerUrlCache.get(form);
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
                .queryActiveServiceInstances(buildProviderServiceName(form), lastConsulIndexId);
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

    private void updateProviderUrlsCache(String form, ConcurrentHashMap<String, List<Url>> newProtocolPlusPath2Urls, boolean needNotify) {
        if (MapUtils.isNotEmpty(newProtocolPlusPath2Urls)) {
            ConcurrentHashMap<String, List<Url>> oldProtocolPlusPath2Urls = providerUrlCache.putIfAbsent(form, newProtocolPlusPath2Urls);
            for (Map.Entry<String, List<Url>> entry : newProtocolPlusPath2Urls.entrySet()) {
                boolean changed = true;
                if (oldProtocolPlusPath2Urls != null) {
                    List<Url> oldUrls = oldProtocolPlusPath2Urls.get(entry.getKey());
                    List<Url> newUrls = entry.getValue();
                    if (ConsulUtils.isSame(newUrls, oldUrls)) {
                        changed = false;
                        log.trace("No provider changes detected for key: {}", entry.getKey());
                    } else {
                        oldProtocolPlusPath2Urls.put(entry.getKey(), newUrls);
                        log.info("Detected provider changes with previous: {} and current: {}", oldUrls, newUrls);
                    }
                }
                if (changed && needNotify) {
                    notificationThreadPool.execute(new NotifyService(entry.getKey(), entry.getValue()));
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
    public void subscribeAllConsumerChanges(ConsumerProcessable consumerProcessor) {
        consumerChangesMonitorPool.scheduleAtFixedRate(
                () -> {
                    getRegisteredConsumerUrls().forEach(url -> {
                        List<Url> consumerUrls = consulClient.getConsumerUrls(url.getPath());
                        if (!consumerUrls.equals(path2ConsumerUrls.get(url.getPath()))) {
                            consumerProcessor.process(getRegistryUrl(), url.getPath(), consumerUrls);
                            path2ConsumerUrls.put(url.getPath(), consumerUrls);
                        }
                    });
                }, 0, 2, TimeUnit.SECONDS);
    }


    @Override
    public List<Url> getAllProviderUrls() {
        return consulClient.getAllProviderUrls();
    }

    protected Url getUrl() {
        return super.registryUrl;
    }

    private class NotifyService implements Runnable {
        private final String    protocolPlusPath;
        private final List<Url> urls;

        public NotifyService(String protocolPlusPath, List<Url> urls) {
            this.protocolPlusPath = protocolPlusPath;
            this.urls = urls;
        }

        @Override
        public void run() {
            ConcurrentHashMap<Url, ProviderListener> listeners = serviceListeners.get(protocolPlusPath);
            if (listeners != null) {
                synchronized (listeners) {
                    for (Map.Entry<Url, ProviderListener> entry : listeners.entrySet()) {
                        ProviderListener serviceListener = entry.getValue();
                        serviceListener.onNotify(entry.getKey(), getUrl(), urls);
                    }
                }
            } else {
                log.debug("Can NOT found the listeners with key: {}" + protocolPlusPath);
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

    @Override
    public void destroy() {
        notificationThreadPool.shutdown();
        consulStatusUpdater.close();
        log.info("Destroyed consul registry");
    }
}
