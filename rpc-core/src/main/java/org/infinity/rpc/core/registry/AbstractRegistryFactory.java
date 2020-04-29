package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class AbstractRegistryFactory implements RegistryFactory {
    /**
     * Object lock
     */
    private final  Lock                  lock            = new ReentrantLock();
    private static Map<String, Registry> registriesCache = new ConcurrentHashMap<>();

    /**
     * Get or create a registry
     *
     * @param url url
     * @return registry
     */
    @Override
    public Registry getRegistry(Url url) {
        String registryUri = url.getUri();
        try {
            lock.lock();
            Registry registry = registriesCache.get(registryUri);
            if (registry != null) {
                return registry;
            }
            long start = System.currentTimeMillis();
            registry = createRegistry(url);
            long elapsed = System.currentTimeMillis() - start;
            log.debug("Created registry [{}] in {} ms", registry.getClass().getSimpleName(), elapsed);
            if (registry == null) {
                throw new RuntimeException("Failed to create registry for url:" + url);
            }
            registriesCache.put(registryUri, registry);
            return registry;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create registry for url:" + url);
        } finally {
            lock.unlock();
        }
    }
}
