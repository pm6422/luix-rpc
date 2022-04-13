package com.luixtech.luixrpc.core.client.sender.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.client.request.Requestable;
import com.luixtech.luixrpc.core.client.sender.AbstractRequestSender;
import com.luixtech.luixrpc.core.constant.ProtocolConstants;
import com.luixtech.luixrpc.core.server.response.Future;
import com.luixtech.luixrpc.core.server.response.Responseable;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.core.exception.TransportException;
import com.luixtech.luixrpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.luixrpc.core.exchange.client.Client;
import com.luixtech.luixrpc.core.exchange.endpoint.NetworkTransmissionFactory;


/**
 * todo: DefaultRpcReferer
 * One service interface corresponds to one {@link RequestSender} instance.
 * The request sender is created when the provider is active.
 */
@Slf4j
public class RequestSender extends AbstractRequestSender {
    private final NetworkTransmissionFactory networkTransmissionFactory;
    private final Client                     client;

    public RequestSender(String interfaceName, Url providerUrl) {
        super(interfaceName, providerUrl);
        long start = System.currentTimeMillis();
        String name = providerUrl.getOption(ProtocolConstants.NETWORK_TRANSMISSION, ProtocolConstants.NETWORK_TRANSMISSION_VAL_NETTY);
        networkTransmissionFactory = NetworkTransmissionFactory.getInstance(name);
        client = networkTransmissionFactory.createClient(providerUrl);
        // Initialize
        super.init();
        log.info("Initialized request sender [{}] in {} ms", this, System.currentTimeMillis() - start);
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
        networkTransmissionFactory.destroyClient(client, providerUrl);
        log.info("Destroy request sender for provider url {}", providerUrl);
    }

    @Override
    public String toString() {
        return RequestSender.class.getSimpleName().concat(":").concat(interfaceName);
    }
}
