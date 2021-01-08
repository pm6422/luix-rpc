package org.infinity.rpc.core.exchange.request.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.request.AbstractProviderCaller;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Future;
import org.infinity.rpc.core.exchange.response.FutureListener;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.transport.Client;
import org.infinity.rpc.core.exchange.transport.endpoint.EndpointFactory;
import org.infinity.rpc.core.exchange.transport.exception.TransmissionException;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;


/**
 * @param <T>: The interface class of the provider
 */
@Slf4j
public class DefaultProviderCaller<T> extends AbstractProviderCaller<T> {
    protected EndpointFactory endpointFactory;
    protected Client          client;

    public DefaultProviderCaller(Class<T> interfaceClass, Url providerUrl) {
        super(interfaceClass, providerUrl);
        endpointFactory = ServiceLoader.forClass(EndpointFactory.class)
                .load(providerUrl.getParameter(Url.PARAM_ENDPOINT_FACTORY, Url.PARAM_ENDPOINT_FACTORY_DEFAULT_VALUE));
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
        future.addListener(new FutureListener() {
            @Override
            public void operationComplete(Future future) throws Exception {
                processingCount.decrementAndGet();
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return client.isActive();
    }

    @Override
    public void destroy() {
        endpointFactory.safeReleaseResource(client, providerUrl);
        log.info("DefaultRpcReferer destory client: url={}" + providerUrl);
    }
}
