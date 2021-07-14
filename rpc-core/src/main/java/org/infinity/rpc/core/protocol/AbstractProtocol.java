package org.infinity.rpc.core.protocol;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.client.sender.impl.RequestSender;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * todo: AbstractProtocol
 */
@Slf4j
public abstract class AbstractProtocol implements Protocol {
    protected final Map<String, Exportable> exporterMap = new ConcurrentHashMap<>();

    @Override
    public Exportable export(Url providerUrl) {
        if (providerUrl == null) {
            throw new RpcFrameworkException("Url must NOT be null!");
        }

        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerUrl);
        synchronized (exporterMap) {
            Exportable exporter = exporterMap.get(protocolKey);
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
    protected abstract Exportable doExport(Url providerUrl);

    @Override
    public void destroy() {
        Iterator<Map.Entry<String, Exportable>> iterator = exporterMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Exportable> next = iterator.next();
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

    public Map<String, Exportable> getExporterMap() {
        return exporterMap;
    }
}
