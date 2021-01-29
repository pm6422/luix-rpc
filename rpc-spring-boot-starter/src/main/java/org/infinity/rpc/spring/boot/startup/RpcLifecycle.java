package org.infinity.rpc.spring.boot.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.exchange.client.stub.ConsumerStub;
import org.infinity.rpc.core.exchange.client.stub.ConsumerStubHolder;
import org.infinity.rpc.core.exchange.server.stub.ProviderStub;
import org.infinity.rpc.core.exchange.server.stub.ProviderStubHolder;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.switcher.impl.SwitcherService;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.utilities.destory.ShutdownHook;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.infinity.rpc.spring.boot.utils.JarUtils.readJarVersion;

/**
 * Used to start and stop the RPC server
 */
@Slf4j
public class RpcLifecycle {
    /**
     * The start flag used to identify whether the RPC server already started.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    /**
     * The stop flag used to identify whether the RPC server already stopped.
     */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Prevent instantiation of it outside the class
     */
    private RpcLifecycle() {
    }

    /**
     * Get the singleton instance
     *
     * @return singleton instance {@link RpcLifecycle}
     */
    public static RpcLifecycle getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * The singleton instance holder static inner class
     */
    private static class SingletonHolder {
        // static variable will be instantiated on class loading.
        private static final RpcLifecycle INSTANCE = new RpcLifecycle();
    }

    public AtomicBoolean getStarted() {
        return started;
    }

    public AtomicBoolean getStopped() {
        return stopped;
    }

    /**
     * Start the RPC server
     *
     * @param infinityProperties RPC configuration properties
     */
    public void start(InfinityProperties infinityProperties) {
        if (!started.compareAndSet(false, true)) {
            // already started
            return;
        }
        log.info("Starting the RPC server");
        registerShutdownHook();
        publish(infinityProperties);
        subscribe(infinityProperties);
        log.info("Started the RPC server");
    }

    /**
     * Register the shutdown hook to system runtime
     */
    private void registerShutdownHook() {
        ShutdownHook.register();
    }

    /**
     * Publish RPC providers to registries
     *
     * @param infinityProperties RPC configuration properties
     */
    private void publish(InfinityProperties infinityProperties) {
        infinityProperties.getRegistryConfigs().forEach(registryConfig -> {
            Registry registry = registryConfig.getRegistryImpl();
            // Publish application first
            publishApplication(infinityProperties, registryConfig, registry);
            // Publish providers next
            publishProviders(infinityProperties, registryConfig, registry);
        });
    }

    private void publishApplication(InfinityProperties infinityProperties, RegistryConfig registryConfig, Registry registry) {
        ApplicationExtConfig application = getApplicationExtConfig(infinityProperties.getApplication());
        registry.registerApplication(application);
        log.debug("Registered RPC server application [{}] to registry [{}]",
                infinityProperties.getApplication().getName(), registryConfig.getName());
    }

    private void publishProviders(InfinityProperties infinityProperties, RegistryConfig registryConfig, Registry registry) {
        Map<String, ProviderStub<?>> providerStubs = ProviderStubHolder.getInstance().getStubs();
        if (MapUtils.isEmpty(providerStubs)) {
            log.info("No RPC service providers found to register to registry [{}]!", registryConfig.getName());
            return;
        }
        providerStubs.forEach((name, providerStub) -> {
            // Register providers
            providerStub.register(infinityProperties.getApplication(), infinityProperties.getAvailableProtocol(),
                    registryConfig, infinityProperties.getProvider(), registry);
        });

        SwitcherService.getInstance().setValue(SwitcherService.REGISTRY_HEARTBEAT_SWITCHER, true);
    }

    private ApplicationExtConfig getApplicationExtConfig(ApplicationConfig applicationConfig) {
        ApplicationExtConfig application = new ApplicationExtConfig(applicationConfig);
        application.setInfinityRpcVersion(readJarVersion());
        // Override the old data every time
        application.setStartTime(DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));
        application.setActive(true);
        return application;
    }

    /**
     * Subscribe provider from registries
     *
     * @param infinityProperties RPC configuration properties
     */
    private void subscribe(InfinityProperties infinityProperties) {
        List<ConsumerStub<?>> consumerStubs = ConsumerStubHolder.getInstance().getStubs();
        if (CollectionUtils.isEmpty(consumerStubs)) {
            log.info("No RPC service consumers found from registry!");
            return;
        }
        infinityProperties.getRegistryConfigs().forEach(registryConfig -> consumerStubs.forEach((consumerStub) -> {
            consumerStub.mergeAttributes(infinityProperties.getAvailableProtocol(),
                    registryConfig, infinityProperties.getConsumer());
            consumerStub.subscribeFromRegistries(registryConfig.getRegistryUrl());
        }));
    }

    /**
     * Stop the RPC server
     *
     * @param infinityProperties RPC configuration properties
     */
    public void destroy(InfinityProperties infinityProperties) {
        if (!started.compareAndSet(true, false) || !stopped.compareAndSet(false, true)) {
            // not yet started or already stopped
            return;
        }

        infinityProperties.getRegistryConfigs().forEach(registryConfig -> {
            //unregisterApplication(registryUrls);
            unregisterProviders(registryConfig.getRegistryUrl());
        });
    }

    /**
     * Unregister RPC providers from registry
     */
    private void unregisterProviders(Url... registryUrls) {
        ProviderStubHolder.getInstance().getStubs().forEach((name, stub) -> stub.unregister(registryUrls));
    }
}
