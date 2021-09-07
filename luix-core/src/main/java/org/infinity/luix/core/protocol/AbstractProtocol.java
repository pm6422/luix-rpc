package org.infinity.luix.core.protocol;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.server.exposer.Exposable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;
import org.infinity.luix.core.client.sender.Sendable;
import org.infinity.luix.core.client.sender.impl.RequestSender;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * todo: AbstractProtocol
 */
@Slf4j
public abstract class AbstractProtocol implements Protocol {
    protected final Map<String, Exposable> exposedProviders = new ConcurrentHashMap<>();

    @Override
    public Exposable expose(Url providerUrl) {
        if (providerUrl == null) {
            throw new RpcFrameworkException("Url must NOT be null!");
        }

        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerUrl);
        synchronized (exposedProviders) {
            Exposable exposer = exposedProviders.get(protocolKey);
            if (exposer != null) {
                throw new RpcFrameworkException("Can NOT re-expose service [" + providerUrl + "]");
            }

            exposer = doExpose(providerUrl);
            exposer.init();

            // todo: check useless statement
            exposedProviders.put(protocolKey, exposer);
            log.info("Exposed service [{}]", providerUrl);
            return exposer;
        }
    }

    @Override
    public Sendable createSender(String interfaceName, Url providerUrl) {
        // todo: create different caller associated with the protocol
        return new RequestSender(interfaceName, providerUrl);
    }

    /**
     * Do create exposer
     *
     * @param providerUrl provider url
     * @return exposer
     */
    protected abstract Exposable doExpose(Url providerUrl);

    @Override
    public void destroy() {
        Iterator<Map.Entry<String, Exposable>> iterator = exposedProviders.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Exposable> next = iterator.next();
            if (next.getValue() != null) {
                try {
                    next.getValue().destroy();
                    log.info("Destroyed [" + next.getValue() + "]");
                } catch (Throwable t) {
                    log.error("Failed to destroy [" + next.getValue() + "]", t);
                }
            }
            iterator.remove();
        }
    }

    public Map<String, Exposable> getExposedProviders() {
        return exposedProviders;
    }
}
