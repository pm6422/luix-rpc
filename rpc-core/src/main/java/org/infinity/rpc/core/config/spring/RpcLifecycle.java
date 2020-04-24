package org.infinity.rpc.core.config.spring;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityRpcProperties;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.server.ProviderWrapperHolder;

import java.util.Arrays;
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
     * Prohibit instantiate an instance
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
     * @param rpcProperties RPC configuration properties
     */
    public void start(InfinityRpcProperties rpcProperties) {
        if (!started.compareAndSet(false, true)) {
            // already started
            return;
        }
        log.info("Starting the RPC server");
        initConfig();
        registerProviders(rpcProperties);
        // referProviders();
        log.info("Started the RPC server");
    }

    /**
     * Initialize the RPC server
     */
    private void initConfig() {
    }

    /**
     * Register RPC providers to registry
     *
     * @param rpcProperties RPC configuration properties
     */
    private void registerProviders(InfinityRpcProperties rpcProperties) {
        ProviderWrapperHolder.getInstance().getWrappers().forEach((name, providerWrapper) -> {
            List<Url> registryUrls = Arrays.asList(Url.of(rpcProperties.getRegistry().getProtocol(), rpcProperties.getRegistry().getHost(), rpcProperties.getRegistry().getPort()));
            Url providerUrl = Url.of(rpcProperties.getRegistry().getProtocol(), rpcProperties.getRegistry().getHost(), rpcProperties.getRegistry().getPort());
            providerWrapper.register(registryUrls, providerUrl);
        });
    }

    /**
     * Stop the RPC server
     *
     * @param rpcProperties RPC configuration properties
     */
    public void stop(InfinityRpcProperties rpcProperties) {
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
            providerWrapper.unregister();
        });
    }
}
