package org.infinity.rpc.core.exchange.request.impl;

import org.infinity.rpc.core.exchange.request.AbstractProviderCaller;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.url.Url;

import java.util.concurrent.Future;

/**
 *
 * @param <T>: The interface class of the provider
 */
public class DefaultProviderCaller<T> extends AbstractProviderCaller<T> {

    public DefaultProviderCaller(Class<T> interfaceClass, Url providerUrl) {
        super(interfaceClass, providerUrl);
    }

    @Override
    protected boolean doInit() {
        // todo: open client
        return true;
    }

    @Override
    protected void reduceProcessingCount(Requestable request, Responseable response) {
        if (response == null || !(response instanceof Future)) {
            processingCount.decrementAndGet();
            return;
        }
//
//        Future future = (Future) response;
//        future.addListener(new FutureListener() {
//            @Override
//            public void operationComplete(Future future) throws Exception {
//                processingCount.decrementAndGet();
//            }
//        });
    }

    @Override
    protected Responseable doCall(Requestable request) {
//        try {
        request.addAttachment(Url.PARAM_GROUP, providerUrl.getGroup());

        return null;
//            return client.request(request);
//        } catch (TransportException exception) {
//            throw new MotanServiceException("DefaultRpcReferer call Error: url=" + url.getUri(), exception);
//        }
    }
}
