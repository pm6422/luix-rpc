package org.infinity.rpc.core.exchange.request.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.request.AbstractProviderCaller;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Future;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.transport.Client;
import org.infinity.rpc.core.exchange.transport.endpoint.EndpointFactory;
import org.infinity.rpc.core.exchange.transport.exception.TransmissionException;
import org.infinity.rpc.core.url.Url;


/**
 * @param <T>: The interface class of the provider
 */
@Slf4j
public class DefaultProviderCaller<T> extends AbstractProviderCaller<T> {
    protected EndpointFactory endpointFactory;
    protected Client          client;

    public DefaultProviderCaller(Class<T> interfaceClass, Url providerUrl) {
        super(interfaceClass, providerUrl);
        String endpointFactoryName = providerUrl.getParameter(Url.PARAM_ENDPOINT_FACTORY, Url.PARAM_ENDPOINT_FACTORY_DEFAULT_VALUE);
        endpointFactory = EndpointFactory.getInstance(endpointFactoryName);
        if (endpointFactory == null) {
            throw new RpcFrameworkException("Endpoint factory [" + endpointFactoryName + "] must not be null!");
        }
        client = endpointFactory.createClient(providerUrl);
    }

    @Override
    protected boolean doInit() {
        return client.open();
    }

    @Override
    protected Responseable doCall(Requestable request) {
        try {
            // 为了能够实现跨group请求，需要使用server端的group。
            request.addAttachment(Url.PARAM_GROUP, providerUrl.getGroup());
            return client.request(request);
        } catch (TransmissionException exception) {
            throw new RpcServiceException("DefaultRpcReferer call Error: url=" + providerUrl.getUri(), exception);
        }
    }

    @Override
    protected void reduceProcessingCount(Requestable request, Responseable response) {
        if (response == null || !(response instanceof Future)) {
            processingCount.decrementAndGet();
            return;
        }
        Future future = (Future) response;
        future.addListener(future1 -> processingCount.decrementAndGet());
    }

    @Override
    public boolean isAvailable() {
        return client.isActive();
    }

    @Override
    public void destroy() {
        endpointFactory.safeReleaseResource(client, providerUrl);
        log.info("DefaultRpcReferer destory client: url {}", providerUrl);
    }
}
