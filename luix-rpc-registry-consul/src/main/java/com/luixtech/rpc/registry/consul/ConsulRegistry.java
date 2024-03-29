package com.luixtech.rpc.registry.consul;

import com.luixtech.rpc.core.listener.GlobalConsumerDiscoveryListener;
import com.luixtech.rpc.core.registry.FailbackAbstractRegistry;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.registry.consul.utils.ConsulUtils;
import com.luixtech.utilities.lang.Destroyable;
import com.luixtech.utilities.lang.ShutdownHook;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.luixtech.rpc.core.constant.RegistryConstants.DISCOVERY_INTERVAL;
import static com.luixtech.rpc.core.constant.RegistryConstants.DISCOVERY_INTERVAL_VAL_DEFAULT;

@Slf4j
@ThreadSafe
public class ConsulRegistry extends FailbackAbstractRegistry implements Destroyable {

    /**
     * Consul client
     */
    private final ConsulHttpClient                     consulHttpClient;
    /**
     * Consul service instance status updater
     */
    private final ConsulStatusUpdater                  consulStatusUpdater;
    /**
     * Key: consumer path
     * Value: consumerUrls
     */
    private final ConcurrentHashMap<String, List<Url>> path2ConsumerUrls          = new ConcurrentHashMap<>();
    /**
     * Consumer changes discovery thread pool
     */
    private final ScheduledExecutorService             consumerChangesMonitorPool = Executors.newSingleThreadScheduledExecutor();

    public ConsulRegistry(Url registryUrl, ConsulHttpClient consulHttpClient) {
        super(registryUrl);
        this.consulHttpClient = consulHttpClient;
        consulStatusUpdater = new ConsulStatusUpdater(consulHttpClient);
        consulStatusUpdater.start();
        DiscoverProviderThread discoverProviderThread = new DiscoverProviderThread(
                this.registryUrl.getIntOption(DISCOVERY_INTERVAL, DISCOVERY_INTERVAL_VAL_DEFAULT));
        discoverProviderThread.setDaemon(true);
        discoverProviderThread.start();
        ShutdownHook.add(this);
        log.info("Initialized consul registry");
    }

    @Override
    protected void doRegister(Url url) {
        ConsulService service = ConsulService.byUrl(url);
        consulHttpClient.registerService(service);
    }

    @Override
    protected void doDeregister(Url url) {
        ConsulService service = ConsulService.byUrl(url);
        consulHttpClient.deregisterService(service.getInstanceId());
    }

    @Override
    protected void doActivate(Url url) {
        if (url == null) {
            // Activate all service instances
            consulStatusUpdater.updateStatus(true);
        } else {
            // Activate specified service instance
            consulStatusUpdater.activate(ConsulUtils.buildInstanceId(url));
        }
    }

    @Override
    protected void doDeactivate(Url url) {
        if (url == null) {
            // Deactivate all service instances
            consulStatusUpdater.updateStatus(false);
        } else {
            // Deactivate specified service instance
            consulStatusUpdater.deactivate(ConsulUtils.buildInstanceId(url));
        }
    }

    @Override
    public List<Url> doDiscoverActive(Url consumerUrl) {
        return consulHttpClient.find(ConsulUtils.CONSUL_PROVIDER_SERVICE_NAME, consumerUrl.getPath(), true);
    }

    @Override
    public List<Url> discoverProviders() {
        return consulHttpClient.find(ConsulUtils.CONSUL_PROVIDER_SERVICE_NAME);
    }

    @Override
    public void subscribe(GlobalConsumerDiscoveryListener listener) {
        consumerChangesMonitorPool.scheduleAtFixedRate(() -> this.discoverConsumerChanges(listener), 0, 2, TimeUnit.SECONDS);
    }

    private void discoverConsumerChanges(GlobalConsumerDiscoveryListener listener) {
        try {
            Map<String, List<Url>> currentPath2ConsumerUrls =
                    consulHttpClient.findActive(ConsulUtils.CONSUL_CONSUMER_SERVICE_NAME)
                            .stream()
                            .collect(Collectors.groupingBy(Url::getPath));

            CollectionUtils.union(currentPath2ConsumerUrls.keySet(), path2ConsumerUrls.keySet())
                    .forEach(path -> {
                        synchronized (path.intern()) {
                            List<Url> oldConsumerUrls = path2ConsumerUrls.get(path);
                            List<Url> newConsumerUrls = currentPath2ConsumerUrls.get(path);
                            if (!Url.isSame(newConsumerUrls, oldConsumerUrls)) {
                                listener.onNotify(getRegistryUrl(), path, newConsumerUrls);
                                if (CollectionUtils.isNotEmpty(newConsumerUrls)) {
                                    path2ConsumerUrls.put(path, newConsumerUrls);
                                } else {
                                    path2ConsumerUrls.remove(path);
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to discover consumer changes", e);
        }
    }

    @Override
    public void unsubscribe(GlobalConsumerDiscoveryListener listener) {
        consumerChangesMonitorPool.shutdown();
    }

    private class DiscoverProviderThread extends Thread {
        /**
         * Service discovery interval in milliseconds
         */
        private final int discoverInterval;

        public DiscoverProviderThread(int discoverInterval) {
            this.discoverInterval = discoverInterval;
        }

        @Override
        public void run() {
            log.info("Started discover provider changes thread with interval: " + discoverInterval + "ms");
            while (true) {
                try {
                    sleep(discoverInterval);
                    Map<String, List<Url>> currentPath2ProviderUrls =
                            consulHttpClient.findActive(ConsulUtils.CONSUL_PROVIDER_SERVICE_NAME)
                                    .stream()
                                    .collect(Collectors.groupingBy(Url::getPath));

                    CollectionUtils.union(currentPath2ProviderUrls.keySet(), path2ProviderUrls.keySet())
                            .forEach(path -> compareAndUpdateChanges(path, currentPath2ProviderUrls.get(path)));
                } catch (Throwable e) {
                    log.error("Failed to discover providers!", e);
                    try {
                        Thread.sleep(2_000);
                    } catch (InterruptedException ignored) {
                        // Leave blank intentionally
                    }
                }
            }
        }

        private void compareAndUpdateChanges(String path, List<Url> newProviderUrls) {
            synchronized (path.intern()) {
                List<Url> oldProviderUrls = path2ProviderUrls.get(path);
                if (Url.isSame(newProviderUrls, oldProviderUrls)) {
                    log.trace("No provider changes discovered for path: {}", path);
                } else {
                    log.info("Discovered provider changes of path: {} with previous: {} and current: {}",
                            path, oldProviderUrls, newProviderUrls);
                    updateAndNotify(path, newProviderUrls);
                }
            }
        }
    }

    @Override
    public void destroy() {
        notifyProviderChangeThreadPool.shutdown();
        consumerChangesMonitorPool.shutdown();
        consulStatusUpdater.close();
        log.info("Destroyed consul registry");
    }
}
