package org.infinity.rpc.spring.boot.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.infinity.rpc.core.config.ApplicationExtConfig;
import org.infinity.rpc.core.exchange.client.stub.ConsumerStub;
import org.infinity.rpc.core.exchange.client.stub.ConsumerStubHolder;
import org.infinity.rpc.core.exchange.server.stub.ProviderStub;
import org.infinity.rpc.core.exchange.server.stub.ProviderStubHolder;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
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
        registerApplication(infinityProperties);
        registerProviders(infinityProperties);
        initConsumers(infinityProperties);
        log.info("Started the RPC server");
    }

    /**
     * Register the shutdown hook to system runtime
     */
    private void registerShutdownHook() {
        ShutdownHook.register();
    }

    /**
     * Register application information to registry
     *
     * @param infinityProperties configuration properties
     */
    private void registerApplication(InfinityProperties infinityProperties) {
        // Register provider URL to all the registries
        Registry registry = infinityProperties.getRegistry().getRegistryImpl();
        ApplicationExtConfig application = new ApplicationExtConfig(infinityProperties.getApplication());
        application.setInfinityRpcVersion(readJarVersion());
        // Override the old data every time
        application.setLatestRegisteredTime(DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));
        registry.registerApplication(application);
        log.debug("Registered RPC server application [{}] to registry", infinityProperties.getApplication().getName());
    }

    /**
     * Register RPC providers to registry
     *
     * @param infinityProperties RPC configuration properties
     */
    private void registerProviders(InfinityProperties infinityProperties) {
        Map<String, ProviderStub<?>> stubs = ProviderStubHolder.getInstance().getStubs();
        if (MapUtils.isEmpty(stubs)) {
            log.info("No RPC service providers found for registering to registry!");
            return;
        }
        stubs.forEach((name, stub) -> {
            Url providerUrl = createProviderUrl(infinityProperties, stub);
            stub.setUrl(providerUrl);
            // DO the providers registering
            stub.publishToRegistries(infinityProperties.getApplication().getName(), providerUrl,
                    infinityProperties.getRegistry().getRegistryUrl());
        });
    }

    /**
     * Create provider url and merge high priority properties to provider stub
     *
     * @param infinityProperties configuration properties
     * @param providerStub       provider stub instance
     * @return provider url
     */
    private Url createProviderUrl(InfinityProperties infinityProperties, ProviderStub<?> providerStub) {
        String protocol = defaultIfEmpty(providerStub.getProtocol(), infinityProperties.getProtocol().getName());
        int port = infinityProperties.getProtocol().getPort();
        String group = defaultIfEmpty(providerStub.getGroup(), infinityProperties.getProvider().getGroup());
        String version = defaultIfEmpty(providerStub.getVersion(), infinityProperties.getProvider().getVersion());
        Url providerUrl = Url.providerUrl(protocol, port, providerStub.getInterfaceName(), group, version);

        providerUrl.addParameter(Url.PARAM_APP, infinityProperties.getApplication().getName());

        boolean checkHealth = toBooleanDefaultIfNull(providerStub.getCheckHealth(),
                infinityProperties.getProvider().isCheckHealth());
        providerUrl.addParameter(Url.PARAM_CHECK_HEALTH, String.valueOf(checkHealth));
        providerStub.setCheckHealth(checkHealth);

        String checkHealthFactory = defaultIfEmpty(providerStub.getCheckHealthFactory(),
                infinityProperties.getProvider().getCheckHealthFactory());
        providerUrl.addParameter(Url.PARAM_CHECK_HEALTH_FACTORY, checkHealthFactory);
        providerStub.setCheckHealthFactory(checkHealthFactory);

        int requestTimeout = Integer.MAX_VALUE != providerStub.getRequestTimeout() ? providerStub.getRequestTimeout()
                : infinityProperties.getProvider().getRequestTimeout();
        providerUrl.addParameter(Url.PARAM_REQUEST_TIMEOUT, String.valueOf(requestTimeout));
        providerStub.setRequestTimeout(requestTimeout);

        int maxRetries = Integer.MAX_VALUE != providerStub.getMaxRetries() ? providerStub.getMaxRetries()
                : infinityProperties.getProvider().getMaxRetries();
        providerUrl.addParameter(Url.PARAM_MAX_RETRIES, String.valueOf(maxRetries));
        providerStub.setMaxRetries(maxRetries);

        return providerUrl;
    }

    private void initConsumers(InfinityProperties infinityProperties) {
        Map<String, ConsumerStub<?>> stubs = ConsumerStubHolder.getInstance().getStubs();
        if (MapUtils.isEmpty(stubs)) {
            return;
        }
        stubs.forEach((name, stub) -> {
            mergeConsumerAttributes(stub, infinityProperties);
            stub.subscribeFromRegistries(infinityProperties.getRegistry().getRegistryUrl());
        });
    }

    private void mergeConsumerAttributes(ConsumerStub<?> stub, InfinityProperties infinityProperties) {
        if (StringUtils.isEmpty(stub.getRegistry())) {
            stub.setRegistry(infinityProperties.getRegistry().getName());
        }
        if (StringUtils.isEmpty(stub.getProtocol())) {
            stub.setProtocol(infinityProperties.getProtocol().getName());
        }
        if (StringUtils.isEmpty(stub.getCluster())) {
            stub.setCluster(infinityProperties.getConsumer().getCluster());
        }
        if (StringUtils.isEmpty(stub.getFaultTolerance())) {
            stub.setFaultTolerance(infinityProperties.getConsumer().getFaultTolerance());
        }
        if (StringUtils.isEmpty(stub.getLoadBalancer())) {
            stub.setLoadBalancer(infinityProperties.getConsumer().getLoadBalancer());
        }
        if (StringUtils.isEmpty(stub.getGroup())) {
            stub.setGroup(infinityProperties.getConsumer().getGroup());
        }
        if (StringUtils.isEmpty(stub.getVersion())) {
            stub.setVersion(infinityProperties.getConsumer().getVersion());
        }
        if (stub.getCheckHealth() == null) {
            stub.setCheckHealth(infinityProperties.getConsumer().isCheckHealth());
        }
        if (StringUtils.isEmpty(stub.getCheckHealthFactory())) {
            stub.setCheckHealthFactory(infinityProperties.getConsumer().getCheckHealthFactory());
        }
        if (Integer.MAX_VALUE == stub.getRequestTimeout()) {
            stub.setRequestTimeout(infinityProperties.getConsumer().getRequestTimeout());
        }
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
//        unregisterApplication(registryUrls);
        unregisterProviders(infinityProperties.getRegistry().getRegistryUrl());
    }

    /**
     * Unregister RPC providers from registry
     */
    private void unregisterProviders(Url... registryUrls) {
        ProviderStubHolder.getInstance().getStubs().forEach((name, stub) -> stub.unregister(registryUrls));
    }
}
