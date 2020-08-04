package org.infinity.rpc.core.protocol;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.url.Url;

@Slf4j
public abstract class AbstractProtocol implements Protocol {
    @Override
    public <T> Requester<T> createRequester(Class<T> interfaceClass, Url providerUrl) {
        if (providerUrl == null) {
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " refer Error: url is null",
                    RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        if (interfaceClass == null) {
            throw new RpcFrameworkException(this.getClass().getSimpleName() + " refer Error: class is null, url=" + providerUrl,
                    RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        long start = System.currentTimeMillis();
        Requester<T> requester = doCreate(interfaceClass, providerUrl);
        requester.init();

        log.info(this.getClass().getSimpleName() + " refer Success: url=" + providerUrl + ", cost:" + (System.currentTimeMillis() - start));
        return requester;
    }

    protected abstract <T> Requester<T> doCreate(Class<T> interfaceClass, Url providerUrl);

    @Override
    public void destroy() {

    }
}
