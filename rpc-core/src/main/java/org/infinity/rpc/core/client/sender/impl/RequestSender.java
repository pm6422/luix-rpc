package org.infinity.rpc.core.client.sender.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.sender.AbstractRequestSender;
import org.infinity.rpc.core.exception.TransportException;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.exchange.client.Client;
import org.infinity.rpc.core.exchange.endpoint.EndpointFactory;
import org.infinity.rpc.core.server.response.Future;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;

import static org.infinity.rpc.core.constant.ProtocolConstants.ENDPOINT_FACTORY;
import static org.infinity.rpc.core.constant.ProtocolConstants.ENDPOINT_FACTORY_VAL_NETTY;


/**
 * todo: DefaultRpcReferer
 * One {@link RequestSender} instance for one service interface.
 * The request sender is created when the provider is active.
 */
@Slf4j
public class RequestSender extends AbstractRequestSender {
    private final EndpointFactory endpointFactory;
    private final Client          client;

    public RequestSender(String interfaceName, Url providerUrl) {
        super(interfaceName, providerUrl);
        long start = System.currentTimeMillis();
        endpointFactory = createEndpointFactory(providerUrl);
        client = endpointFactory.createClient(providerUrl);
        // Initialize
        super.init();
        log.info("Initialized request sender [{}] in {} ms", this, System.currentTimeMillis() - start);
    }

    private EndpointFactory createEndpointFactory(Url providerUrl) {
        final EndpointFactory endpointFactory;
        String name = providerUrl.getOption(ENDPOINT_FACTORY, ENDPOINT_FACTORY_VAL_NETTY);
        endpointFactory = EndpointFactory.getInstance(name);
        return endpointFactory;
    }

    @Override
    protected boolean doInit() {
        return client.open();
    }

    @Override
    protected Responseable doSend(Requestable request) {
        try {
            return client.request(request);
        } catch (TransportException exception) {
            throw new RpcFrameworkException("Failed to call [" + providerUrl.getUri() + "]", exception);
        }
    }

    @Override
    protected void afterSend(Requestable request, Responseable response) {
        if (!(response instanceof Future)) {
            // Sync response
            processingCount.decrementAndGet();
            return;
        }
        Future future = (Future) response;
        future.addListener(f -> processingCount.decrementAndGet());
    }

    @Override
    public boolean isActive() {
        return client.isActive();
    }

    @Override
    public void destroy() {
        endpointFactory.safeReleaseResource(client, providerUrl);
        log.info("Destroy request sender for provider url {}", providerUrl);
    }

    @Override
    public String toString() {
        return RequestSender.class.getSimpleName().concat(":").concat(interfaceName);
    }
}
