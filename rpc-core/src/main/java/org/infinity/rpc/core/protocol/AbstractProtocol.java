package org.infinity.rpc.core.protocol;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.request.ProviderCaller;
import org.infinity.rpc.core.client.request.impl.DefaultProviderCaller;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractProtocol implements Protocol {
    protected final Map<String, Exportable<?>> exporterMap = new ConcurrentHashMap<>();

    @Override
    public <T> ProviderCaller<T> createProviderCaller(String interfaceName, Url providerUrl) {
        if (StringUtils.isEmpty(interfaceName)) {
            throw new RpcFrameworkException("Provider interface must NOT be null!", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        if (providerUrl == null) {
            throw new RpcFrameworkException("Provider url must NOT be null!", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        // todo: create different caller associated with the protocol
        return new DefaultProviderCaller<>(interfaceName, providerUrl);
    }

    @Override
    public <T> Exportable<T> export(ProviderStub<T> providerStub) {
        if (providerStub.getUrl() == null) {
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " export Error: url is null", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }

        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerStub.getUrl());
        synchronized (exporterMap) {
            @SuppressWarnings("unchecked")
            Exportable<T> exporter = (Exportable<T>) exporterMap.get(protocolKey);
            if (exporter != null) {
                throw new RpcFrameworkException(this.getClass().getSimpleName() + " export Error: service already exist, url=" + providerStub.getUrl(),
                        RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
            }

            exporter = createExporter(providerStub);
            exporter.init();

            exporterMap.put(protocolKey, exporter);
            log.info(this.getClass().getSimpleName() + " export Success: url=" + providerStub.getUrl());
            return exporter;
        }
    }

    /**
     * Create exporter
     *
     * @param providerStub provider stub
     * @param <T>          service interface class
     * @return exporter
     */
    protected abstract <T> Exportable<T> createExporter(ProviderStub<T> providerStub);

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
}
