package com.luixtech.luixrpc.core.registry.factory;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.registry.Registry;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.luixrpc.utilities.concurrent.NotThreadSafe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@NotThreadSafe
public abstract class AbstractRegistryFactory implements RegistryFactory {
    /**
     * Object lock
     */
    private final        Lock                  lock           = new ReentrantLock();
    private static final Map<String, Registry> registryPerUri = new ConcurrentHashMap<>();

    /**
     * Get or create a registry
     * <p>
     *
     * @param registryUrl registry url
     * @return registry
     */
    @Override
    public Registry getRegistry(Url registryUrl) {
        String registryUri = registryUrl.getUri();
        try {
            lock.lock();
            Registry registry = registryPerUri.get(registryUri);
            if (registry != null) {
                return registry;
            }
            long start = System.currentTimeMillis();
            registry = createRegistry(registryUrl);
            if (registry == null) {
                throw new RpcFrameworkException("Failed to create registry for url [" + registryUrl + "]");
            }
            long elapsed = System.currentTimeMillis() - start;
            log.debug("Created registry [{}] in {} ms", registry.getClass().getSimpleName(), elapsed);
            registryPerUri.put(registryUri, registry);
            return registry;
        } catch (Exception e) {
            throw new RpcFrameworkException("Failed to create " + registryUrl.getProtocol() + " registry", e);
        } finally {
            lock.unlock();
        }
    }
}
