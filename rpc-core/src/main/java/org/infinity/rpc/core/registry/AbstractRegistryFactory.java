package org.infinity.rpc.core.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractRegistryFactory implements RegistryFactory {
    /**
     * class lock
     */
    private static final Lock                  LOCK       = new ReentrantLock();
    private static       Map<String, Registry> registries = new ConcurrentHashMap<String, Registry>();

    @Override
    public Registry getRegistry(Url url) {
        String registryUri = getRegistryUri(url);
        try {
            LOCK.lock();
            Registry registry = registries.get(registryUri);
            if (registry != null) {
                return registry;
            }
            registry = createRegistry(url);
            if (registry == null) {
                throw new RuntimeException("Create registry false for url:" + url);
            }
            registries.put(registryUri, registry);
            return registry;
        } catch (Exception e) {
            throw new RuntimeException("Create registry false for url:" + url);
        } finally {
            LOCK.unlock();
        }
    }

    private String getRegistryUri(Url url) {
        return url.getUri();
    }

    protected abstract Registry createRegistry(Url url);
}
