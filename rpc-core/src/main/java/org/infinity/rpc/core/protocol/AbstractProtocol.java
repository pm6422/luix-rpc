package org.infinity.rpc.core.protocol;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.sender.Sendable;
import org.infinity.rpc.core.client.sender.impl.DefaultRpcSender;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.server.stub.ProviderStub;
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
    protected final Map<String, Exportable<?>> exporterMap = new ConcurrentHashMap<>();

    @Override
    public <T> Exportable<T> export(ProviderStub<T> providerStub) {
        if (providerStub.getUrl() == null) {
            throw new RpcFrameworkException("Url must NOT be null!");
        }

        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerStub.getUrl());
        synchronized (exporterMap) {
            Exportable<T> exporter = (Exportable<T>) exporterMap.get(protocolKey);
            if (exporter != null) {
                throw new RpcFrameworkException("Can NOT re-export service [" + providerStub.getUrl() + "]");
            }

            exporter = doExport(providerStub);
            exporter.init();

            // todo: check useless statement
            exporterMap.put(protocolKey, exporter);
            log.info("Exported service [{}]", providerStub.getUrl());
            return exporter;
        }
    }

    @Override
    public Sendable refer(String interfaceName, Url providerUrl) {
        if (StringUtils.isEmpty(interfaceName)) {
            throw new RpcFrameworkException("Provider interface must NOT be null!");
        }
        if (providerUrl == null) {
            throw new RpcFrameworkException("Provider url must NOT be null!");
        }
        // todo: create different caller associated with the protocol
        return new DefaultRpcSender(interfaceName, providerUrl);
    }

    /**
     * Do create exporter
     *
     * @param providerStub provider stub
     * @return exporter
     */
    protected abstract <T> Exportable<T> doExport(ProviderStub<T> providerStub);

    @Override
    public void destroy() {
        Iterator<Map.Entry<String, Exportable<?>>> iterator = exporterMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Exportable<?>> next = iterator.next();
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

    public Map<String, Exportable<?>> getExporterMap() {
        return exporterMap;
    }
}
