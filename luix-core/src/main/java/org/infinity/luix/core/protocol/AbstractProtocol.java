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
    protected final Map<String, Exposable> exporterMap = new ConcurrentHashMap<>();

    @Override
    public Exposable export(Url providerUrl) {
        if (providerUrl == null) {
            throw new RpcFrameworkException("Url must NOT be null!");
        }

        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerUrl);
        synchronized (exporterMap) {
            Exposable exporter = exporterMap.get(protocolKey);
            if (exporter != null) {
                throw new RpcFrameworkException("Can NOT re-export service [" + providerUrl + "]");
            }

            exporter = doExport(providerUrl);
            exporter.init();

            // todo: check useless statement
            exporterMap.put(protocolKey, exporter);
            log.info("Exported service [{}]", providerUrl);
            return exporter;
        }
    }

    @Override
    public Sendable createSender(String interfaceName, Url providerUrl) {
        // todo: create different caller associated with the protocol
        return new RequestSender(interfaceName, providerUrl);
    }

    /**
     * Do create exporter
     *
     * @param providerUrl provider url
     * @return exporter
     */
    protected abstract Exposable doExport(Url providerUrl);

    @Override
    public void destroy() {
        Iterator<Map.Entry<String, Exposable>> iterator = exporterMap.entrySet().iterator();
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

    public Map<String, Exposable> getExporterMap() {
        return exporterMap;
    }
}
