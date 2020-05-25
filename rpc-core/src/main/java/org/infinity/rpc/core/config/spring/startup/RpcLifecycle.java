package org.infinity.rpc.core.config.spring.startup;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.server.ProviderWrapper;
import org.infinity.rpc.core.server.ProviderWrapperHolder;
import org.infinity.rpc.core.switcher.DefaultSwitcherService;
import org.infinity.rpc.core.switcher.SwitcherService;
import org.infinity.rpc.utilities.destory.ShutdownHook;
import org.infinity.rpc.utilities.network.NetworkIpUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Used to start and stop the RPC server
 */
@Slf4j
public class RpcLifecycle {
    /**
     * The start flag used to identify whether the RPC server already started.
     */
    private AtomicBoolean started = new AtomicBoolean(false);
    /**
     * The stop flag used to identify whether the RPC server already stopped.
     */
    private AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Prohibit instantiate an instance outside the class
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
        private static final RpcLifecycle INSTANCE = new RpcLifecycle();// static variable will be instantiated on class loading.
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
        initConfig();
        registerShutdownHook();
        registerApplication(infinityProperties, registryUrls);
        registerProviders(infinityProperties, registryUrls);
        // Delayed exposure providers
        DefaultSwitcherService.getInstance().setValue(SwitcherService.REGISTRY_HEARTBEAT_SWITCHER, true);
        // referProviders();
        log.info("Started the RPC server");
        log.info("Starting the netty server");
    }

    /**
     * Initialize the RPC server
     */
    private void initConfig() {
    }

    /**
     * Register the shutdown hook to system runtime
     */
    private void registerShutdownHook() {
        ShutdownHook.register();
    }

    /**
     * Register RPC providers to registry
     *
     * @param infinityProperties RPC configuration properties
     */
    private void registerProviders(InfinityProperties infinityProperties, List<Url> registryUrls) {
        // TODO: consider using the async thread pool to speed up the startup process
        ProviderWrapperHolder.getInstance().getWrappers().forEach((name, providerWrapper) -> {
            Url providerUrl = createProviderUrl(infinityProperties, providerWrapper);
            // DO the providers registering
            providerWrapper.register(infinityProperties.getApplication().toApp(), registryUrls, providerUrl);
        });
    }

    /**
     * Create provider url
     *
     * @param infinityProperties configuration properties
     * @param providerWrapper    provider instance wrapper
     * @return provider url
     */
    private Url createProviderUrl(InfinityProperties infinityProperties, ProviderWrapper providerWrapper) {
        Url providerUrl = Url.of(
                infinityProperties.getProtocol().getName().value(),
                NetworkIpUtils.INTRANET_IP,
                infinityProperties.getProtocol().getPort(),
                providerWrapper.getProviderInterface());

        // Assign values to parameters
        providerUrl.addParameter(Url.PARAM_CHECK_HEALTH, Url.PARAM_CHECK_HEALTH_DEFAULT_VALUE);
        providerUrl.addParameter(Url.PARAM_APP, infinityProperties.getApplication().getName());
        return providerUrl;
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
            RegistryFactory registryFactoryImpl = RegistryFactory.getInstance(registryUrl.getProtocol());
            Registry registry = registryFactoryImpl.getRegistry(registryUrl);
            registry.registerApplication(infinityProperties.getApplication().toApp());
        }
        log.debug("Registered RPC server application [{}] to registry", infinityProperties.getApplication().getName());
    }

    /**
     * Stop the RPC server
     *
     * @param rpcProperties RPC configuration properties
     */
    public void stop(InfinityProperties rpcProperties, List<Url> registryUrls) {
        if (!started.compareAndSet(true, false) || !stopped.compareAndSet(false, true)) {
            // not yet started or already stopped
            return;
        }
        unregisterProviders();
    }

    /**
     * Unregister RPC providers from registry
     */
    private void unregisterProviders() {
        ProviderWrapperHolder.getInstance().getWrappers().forEach((name, providerWrapper) -> {
            // TODO: unregister from registry
            providerWrapper.unregister();
        });
    }
}
