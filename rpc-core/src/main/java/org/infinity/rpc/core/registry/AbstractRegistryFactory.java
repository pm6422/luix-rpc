package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.concurrent.NotThreadSafe;

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
                throw new RpcFrameworkException("Failed to create registry for url [" + registryUrl + "]",
                        RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
            }
            long elapsed = System.currentTimeMillis() - start;
            log.debug("Created registry [{}] in {} ms", registry.getClass().getSimpleName(), elapsed);
            registryPerUri.put(registryUri, registry);
            return registry;
        } catch (Exception e) {
            throw new RpcFrameworkException("Failed to create registry for url [" + registryUrl + "]", e,
                    RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        } finally {
            lock.unlock();
        }
    }
}
