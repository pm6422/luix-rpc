package org.infinity.rpc.core.client.request.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exception.TransportException;
import org.infinity.rpc.core.client.request.AbstractProviderCaller;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Future;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.exchange.client.Client;
import org.infinity.rpc.core.exchange.endpoint.EndpointFactory;
import org.infinity.rpc.core.url.Url;

import static org.infinity.rpc.core.constant.ProtocolConstants.ENDPOINT_FACTORY;
import static org.infinity.rpc.core.constant.ProtocolConstants.ENDPOINT_FACTORY_VAL_NETTY;
import static org.infinity.rpc.core.constant.ServiceConstants.GROUP;


/**
 * todo: DefaultRpcReferer
 * One default provider caller for one service interface.
 * The provider caller is created when the provider is active.
 *
 * @param <T>: The interface class of the provider
 */
@Slf4j
public class DefaultProviderCaller<T> extends AbstractProviderCaller<T> {
    protected EndpointFactory endpointFactory;
    protected Client          client;

    public DefaultProviderCaller(Class<T> interfaceClass, Url providerUrl) {
        super(interfaceClass, providerUrl);
        long start = System.currentTimeMillis();
        String endpointFactoryName = providerUrl.getOption(ENDPOINT_FACTORY, ENDPOINT_FACTORY_VAL_NETTY);
        endpointFactory = EndpointFactory.getInstance(endpointFactoryName);
        if (endpointFactory == null) {
            throw new RpcFrameworkException("Endpoint factory [" + endpointFactoryName + "] must NOT be null!");
        }
        client = endpointFactory.createClient(providerUrl);
        // Initialize
        init();
        log.info("Initialized provider caller [{}] in {} ms", this.toString(), System.currentTimeMillis() - start);
    }

    @Override
    protected boolean doInit() {
        return client.open();
    }

    @Override
    protected Responseable doCall(Requestable request) {
        try {
            // 为了能够实现跨group请求，需要使用server端的group。
            request.addOption(GROUP, providerUrl.getGroup());
            return client.request(request);
        } catch (TransportException exception) {
            throw new RpcServiceException("DefaultRpcReferer call Error: url=" + providerUrl.getUri(), exception);
        }
    }

    @Override
    protected void reduceProcessingCount(Requestable request, Responseable response) {
        if (!(response instanceof Future)) {
            processingCount.decrementAndGet();
            return;
        }
        Future future = (Future) response;
        future.addListener(future1 -> processingCount.decrementAndGet());
    }

    @Override
    public boolean isActive() {
        return client.isActive();
    }

    @Override
    public void destroy() {
        endpointFactory.safeReleaseResource(client, providerUrl);
        log.info("DefaultRpcReferer destroy client: url {}", providerUrl);
    }

    @Override
    public String toString() {
        return DefaultProviderCaller.class.getSimpleName().concat(":").concat(interfaceClass.getName());
    }
}
