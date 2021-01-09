package org.infinity.rpc.core.protocol;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.server.exporter.Exportable;
import org.infinity.rpc.core.config.spring.server.providerwrapper.ProviderWrapper;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractProtocol implements Protocol {
    protected Map<String, Exportable<?>> exporterMap = new ConcurrentHashMap<>();

    @Override
    public <T> ProviderCaller<T> createProviderCaller(Class<T> interfaceClass, Url providerUrl) {
        if (interfaceClass == null) {
            throw new RpcFrameworkException("Provider interface must NOT be null!", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        if (providerUrl == null) {
            throw new RpcFrameworkException("Provider url must NOT be null!", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        long start = System.currentTimeMillis();
        ProviderCaller<T> providerCaller = doCreate(interfaceClass, providerUrl);
        providerCaller.init();
        log.info("Created provider caller for url {} in {} ms", providerUrl, System.currentTimeMillis() - start);
        return providerCaller;
    }

    protected abstract <T> ProviderCaller<T> doCreate(Class<T> interfaceClass, Url providerUrl);

    @Override
    public <T> Exportable<T> export(ProviderWrapper<T> providerWrapper) {
        if (providerWrapper.getUrl() == null) {
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " export Error: url is null", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }

        if (providerWrapper == null) {
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " export Error: provider is null, url=" + providerWrapper.getUrl(),
                    RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }

        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerWrapper.getUrl());
        synchronized (exporterMap) {
            Exportable<T> exporter = (Exportable<T>) exporterMap.get(protocolKey);
            if (exporter != null) {
                throw new RpcFrameworkException(this.getClass().getSimpleName() + " export Error: service already exist, url=" + providerWrapper.getUrl(),
                        RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
            }

            exporter = createExporter(providerWrapper);
            exporter.init();

            exporterMap.put(protocolKey, exporter);
            log.info(this.getClass().getSimpleName() + " export Success: url=" + providerWrapper.getUrl());
            return exporter;
        }
    }

    protected abstract <T> Exportable<T> createExporter(ProviderWrapper<T> providerWrapper);

    @Override
    public void destroy() {

    }
}
