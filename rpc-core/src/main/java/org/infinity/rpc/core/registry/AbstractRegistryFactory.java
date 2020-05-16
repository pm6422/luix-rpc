package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@ThreadSafe
public abstract class AbstractRegistryFactory implements RegistryFactory {
    /**
     * Object lock
     */
    private final  Lock                  lock            = new ReentrantLock();
    private static Map<String, Registry> registriesCache = new ConcurrentHashMap<>();

    /**
     * Get or create a registry
     *
     * @param registryUrl registry url
     * @return registry
     */
    @Override
    public Registry getRegistry(Url registryUrl) {
        String registryUri = registryUrl.getUri();
        try {
            lock.lock();
            Registry registry = registriesCache.get(registryUri);
            if (registry != null) {
                return registry;
            }
            long start = System.currentTimeMillis();
            registry = createRegistry(registryUrl);
            long elapsed = System.currentTimeMillis() - start;
            log.debug("Created registry [{}] in {} ms", registry.getClass().getSimpleName(), elapsed);
            if (registry == null) {
                throw new RuntimeException("Registry must NOT be null!");
            }
            registriesCache.put(registryUri, registry);
            return registry;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create registry for url [" + registryUrl + "]");
        } finally {
            lock.unlock();
        }
    }
}
