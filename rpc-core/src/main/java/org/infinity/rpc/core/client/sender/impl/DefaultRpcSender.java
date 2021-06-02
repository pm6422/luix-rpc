package org.infinity.rpc.core.client.sender.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.sender.AbstractRpcSender;
import org.infinity.rpc.core.client.request.Requestable;
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
 * One instance for one service interface.
 * The provider invoker is created when the provider is active.
 */
@Slf4j
public class DefaultRpcSender extends AbstractRpcSender {
    protected EndpointFactory endpointFactory;
    protected Client          client;

    public DefaultRpcSender(String interfaceName, Url providerUrl) {
        super(interfaceName, providerUrl);
        long start = System.currentTimeMillis();
        String endpointFactoryName = providerUrl.getOption(ENDPOINT_FACTORY, ENDPOINT_FACTORY_VAL_NETTY);
        endpointFactory = EndpointFactory.getInstance(endpointFactoryName);
        if (endpointFactory == null) {
            throw new RpcFrameworkException("Endpoint factory [" + endpointFactoryName + "] must NOT be null!");
        }
        client = endpointFactory.createClient(providerUrl);
        // Initialize
        init();
        log.info("Initialized provider invoker [{}] in {} ms", this, System.currentTimeMillis() - start);
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
        return DefaultRpcSender.class.getSimpleName().concat(":").concat(interfaceName);
    }
}
