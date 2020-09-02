package org.infinity.rpc.core.protocol;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.request.ProtocolRequester;
import org.infinity.rpc.core.url.Url;

@Slf4j
public abstract class AbstractProtocol implements Protocol {
    @Override
    public <T> ProtocolRequester<T> createRequester(Class<T> interfaceClass, Url providerUrl) {
        if (interfaceClass == null) {
            throw new RpcFrameworkException("Provider interface must NOT be null!", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        if (providerUrl == null) {
            throw new RpcFrameworkException("Provider url must NOT be null!", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        long start = System.currentTimeMillis();
        ProtocolRequester<T> protocolRequester = doCreate(interfaceClass, providerUrl);
        protocolRequester.init();
        log.info("Created protocol requester for url {} in {} ms", providerUrl, System.currentTimeMillis() - start);
        return protocolRequester;
    }

    protected abstract <T> ProtocolRequester<T> doCreate(Class<T> interfaceClass, Url providerUrl);

    @Override
    public void destroy() {

    }
}
