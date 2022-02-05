package org.infinity.luix.registry.consul;

import com.ecwid.consul.v1.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.luix.core.listener.client.ConsumerListener;
import org.infinity.luix.core.listener.server.ConsumerProcessable;
import org.infinity.luix.core.registry.FailbackAbstractRegistry;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.registry.consul.utils.ConsulUtils;
import org.infinity.luix.utilities.destory.Destroyable;
import org.infinity.luix.utilities.destory.ShutdownHook;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.infinity.luix.core.constant.RegistryConstants.DISCOVERY_INTERVAL;
import static org.infinity.luix.core.constant.RegistryConstants.DISCOVERY_INTERVAL_VAL_DEFAULT;
import static org.infinity.luix.registry.consul.ConsulService.TAG_PREFIX_PATH;
import static org.infinity.luix.registry.consul.utils.ConsulUtils.CONSUL_PROVIDING_SERVICES_PREFIX;
import static org.infinity.luix.registry.consul.utils.ConsulUtils.CONSUL_TAG_DELIMITER;

@Slf4j
@ThreadSafe
public class ConsulRegistry extends FailbackAbstractRegistry implements Destroyable {

    /**
     * Consul client
     */
    private final LuixConsulClient                     consulClient;
    /**
     * Consul service instance status updater
     */
    private final ConsulStatusUpdater                  consulStatusUpdater;
    /**
     * Service discovery interval in milliseconds
     */
    private final int                                  discoverInterval;
    /**
     * Key: consumer path
     * Value: consumerUrls
     */
    private final ConcurrentHashMap<String, List<Url>> path2ConsumerUrls          = new ConcurrentHashMap<>();
    /**
     *
     */
    private final ScheduledExecutorService             consumerChangesMonitorPool = Executors.newSingleThreadScheduledExecutor();

    public ConsulRegistry(Url registryUrl, LuixConsulClient consulClient) {
        super(registryUrl);
        this.consulClient = consulClient;
        consulStatusUpdater = new ConsulStatusUpdater(consulClient);
        consulStatusUpdater.start();
        discoverInterval = this.registryUrl.getIntOption(DISCOVERY_INTERVAL, DISCOVERY_INTERVAL_VAL_DEFAULT);
        DiscoverProviderThread discoverProviderThread = new DiscoverProviderThread();
        discoverProviderThread.setDaemon(true);
        discoverProviderThread.start();
        ShutdownHook.add(this);
        log.info("Initialized consul registry");
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
    public List<Url> discoverProviders(Url consumerUrl) {
        List<Url> providerUrls = path2ProviderUrls.get(consumerUrl.getPath());
        if (providerUrls == null) {
            // todo: learn how to use the multiple threads
            synchronized (consumerUrl.getPath().intern()) {
                providerUrls = path2ProviderUrls.get(consumerUrl.getPath());
                if (providerUrls == null) {
                    providerUrls = doDiscoverActiveProviders(consumerUrl);
                    compareResults(consumerUrl.getPath(), providerUrls, false);
                }
            }
        }
        return providerUrls;
    }

    private List<Url> doDiscoverActiveProviders(Url consumerUrl) {
        List<Url> providerUrls = new ArrayList<>();
        Response<List<ConsulService>> response;
        if (consumerUrl != null) {
            response = consulClient
                    .queryActiveServiceInstances(CONSUL_PROVIDING_SERVICES_PREFIX,
                            TAG_PREFIX_PATH + CONSUL_TAG_DELIMITER + consumerUrl.getPath());
        } else {
            response = consulClient
                    .queryActiveServiceInstances(CONSUL_PROVIDING_SERVICES_PREFIX);
        }
        if (response != null) {
            List<ConsulService> activeServiceInstances = response.getValue();
            if (CollectionUtils.isNotEmpty(activeServiceInstances)) {
                for (ConsulService activeServiceInstance : activeServiceInstances) {
                    providerUrls.add(ConsulUtils.buildUrl(activeServiceInstance));
                }
            } else {
                if (consumerUrl != null) {
                    log.info("No active providers found on consul registry for consumer url: [{}]", consumerUrl);
                } else {
                    log.info("No active providers found on consul registry");
                }
            }
        }
        return providerUrls;
    }

    private void compareResults(String path, List<Url> newProviderUrls, boolean needNotify) {
        List<Url> oldProviderUrls = path2ProviderUrls.get(path);
        if (Url.isSame(newProviderUrls, oldProviderUrls)) {
            log.trace("No provider changes discovered for path: {}", path);
        } else {
            log.info("Discovered provider changes of path: {} with previous: {} and current: {}",
                    path, oldProviderUrls, newProviderUrls);
            if (needNotify) {
                updateAndNotify(path, newProviderUrls == null ? Collections.emptyList() : newProviderUrls);
            }
        }
    }

    @Override
    protected void subscribeListener(Url consumerUrl, ConsumerListener listener) {
        // Leave blank intentionally
    }

    @Override
    protected void unsubscribeListener(Url consumerUrl, ConsumerListener listener) {
        // Leave blank intentionally
    }

    @Override
    public void subscribeAllConsumerChanges(ConsumerProcessable consumerProcessor) {
        consumerChangesMonitorPool.scheduleAtFixedRate(
                () -> getRegisteredConsumerUrls().forEach(url -> {
                    List<Url> consumerUrls = consulClient.getConsumerUrls(url.getPath());
                    if (!Url.isSame(consumerUrls, path2ConsumerUrls.get(url.getPath()))) {
                        consumerProcessor.process(getRegistryUrl(), url.getPath(), consumerUrls);
                        path2ConsumerUrls.put(url.getPath(), consumerUrls);
                    }
                }), 0, 2, TimeUnit.SECONDS);
    }


    @Override
    public List<Url> getAllProviderUrls() {
        return consulClient.getAllProviderUrls();
    }

    private class DiscoverProviderThread extends Thread {

        @Override
        public void run() {
            log.info("Start discover providers thread with interval: " + discoverInterval + "ms");
            while (true) {
                try {
                    sleep(discoverInterval);
                    Map<String, List<Url>> currentPath2ProviderUrls = doDiscoverActiveProviders(null)
                            .stream()
                            .collect(Collectors.groupingBy(Url::getPath));
                    path2ProviderUrls.keySet()
                            .forEach(path -> compareResults(path, currentPath2ProviderUrls.get(path), true));
                    CollectionUtils.subtract(currentPath2ProviderUrls.keySet(), path2ProviderUrls.keySet())
                            .forEach(path -> compareResults(path, currentPath2ProviderUrls.get(path), true));

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
