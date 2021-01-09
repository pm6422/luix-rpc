package org.infinity.rpc.core.protocol;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.config.spring.server.exporter.Exportable;
import org.infinity.rpc.core.url.Url;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractProtocol implements Protocol {
    protected ConcurrentHashMap<String, Exportable<?>> exporterMap = new ConcurrentHashMap<String, Exportable<?>>();

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
    public void destroy() {

    }
}
