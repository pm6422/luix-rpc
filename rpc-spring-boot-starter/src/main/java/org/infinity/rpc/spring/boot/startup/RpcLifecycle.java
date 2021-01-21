package org.infinity.rpc.spring.boot.startup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.exchange.client.stub.ConsumerStub;
import org.infinity.rpc.core.exchange.client.stub.ConsumerStubHolder;
import org.infinity.rpc.core.exchange.server.stub.ProviderStub;
import org.infinity.rpc.core.exchange.server.stub.ProviderStubHolder;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

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
    public void start(InfinityProperties infinityProperties, List<Url> registryUrls) {
        if (!started.compareAndSet(false, true)) {
            // already started
            return;
        }
        log.info("Starting the RPC server");
        initConfig(infinityProperties);
        registerShutdownHook();
        registerApplication(infinityProperties, registryUrls);
        registerProviders(infinityProperties, registryUrls);
        initConsumers(infinityProperties, registryUrls);
        // referProviders();
        log.info("Started the RPC server");
    }


    /**
     * Initialize the RPC server
     */
    private void initConfig(InfinityProperties infinityProperties) {
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
     * @param registryUrls       registry urls
     */
    private void registerApplication(InfinityProperties infinityProperties, List<Url> registryUrls) {
        for (Url registryUrl : registryUrls) {
            // Register provider URL to all the registries
            RegistryFactory registryFactory = RegistryFactory.getInstance(registryUrl.getProtocol());
            Registry registry = registryFactory.getRegistry(registryUrl);
            registry.registerApplication(infinityProperties.getApplication().toApp());
        }
        log.debug("Registered RPC server application [{}] to registry", infinityProperties.getApplication().getName());
    }

    /**
     * Register RPC providers to registry
     *
     * @param infinityProperties RPC configuration properties
     */
    private void registerProviders(InfinityProperties infinityProperties, List<Url> registryUrls) {
        Map<String, ProviderStub<?>> stubs = ProviderStubHolder.getInstance().getStubs();
        if (MapUtils.isEmpty(stubs)) {
            log.info("No RPC service providers found for registering to registry!");
            return;
        }
        stubs.forEach((name, stub) -> {
            Url providerUrl = createProviderUrl(infinityProperties, stub);
            stub.setUrl(providerUrl);
            // DO the providers registering
            stub.register(infinityProperties.getApplication().toApp(), registryUrls, providerUrl);
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

    private void initConsumers(InfinityProperties infinityProperties, List<Url> registryUrls) {
        Map<String, ConsumerStub<?>> stubs = ConsumerStubHolder.getInstance().getStubs();
        if (MapUtils.isEmpty(stubs)) {
            return;
        }
        stubs.forEach((name, stub) -> {
            mergeAttributes(stub, infinityProperties);
            stub.init(registryUrls);
        });
    }

    private void mergeAttributes(ConsumerStub<?> stub, InfinityProperties infinityProperties) {
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
        if(Integer.MAX_VALUE == stub.getRequestTimeout()) {
            stub.setRequestTimeout(infinityProperties.getConsumer().getRequestTimeout());
        }
    }

    /**
     * Stop the RPC server
     *
     * @param rpcProperties RPC configuration properties
     */
    public void destroy(InfinityProperties rpcProperties, List<Url> registryUrls) {
        if (!started.compareAndSet(true, false) || !stopped.compareAndSet(false, true)) {
            // not yet started or already stopped
            return;
        }
//        unregisterApplication(registryUrls);
        unregisterProviders(registryUrls);
    }

    /**
     * Unregister RPC providers from registry
     */
    private void unregisterProviders(List<Url> registryUrls) {
        ProviderStubHolder.getInstance().getStubs().forEach((name, stub) -> stub.unregister(registryUrls));
    }
}
