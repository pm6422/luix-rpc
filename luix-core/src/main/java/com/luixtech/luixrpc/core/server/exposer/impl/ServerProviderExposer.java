package com.luixtech.luixrpc.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.exchange.endpoint.NetworkTransmissionFactory;
import com.luixtech.luixrpc.core.exchange.server.Server;
import com.luixtech.luixrpc.core.server.exposer.AbstractProviderExposer;
import com.luixtech.luixrpc.core.server.handler.impl.ServerInvocationHandler;
import com.luixtech.luixrpc.core.server.handler.impl.ProtectedServerInvocationHandler;
import com.luixtech.luixrpc.core.url.Url;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.luixtech.luixrpc.core.constant.ProtocolConstants.NETWORK_TRANSMISSION;
import static com.luixtech.luixrpc.core.constant.ProtocolConstants.NETWORK_TRANSMISSION_VAL_NETTY;

@Slf4j
public class ServerProviderExposer extends AbstractProviderExposer {

    /**
     * 多个服务可在不同端口进行服务暴露
     */
    protected static final Map<String, ServerInvocationHandler> ADDRESS_2_PROVIDER_INVOCATION_HANDLER = new ConcurrentHashMap<>();
    protected Server                     server;
    protected NetworkTransmissionFactory networkTransmissionFactory;

    public ServerProviderExposer(Url providerUrl) {
        super(providerUrl);

        ServerInvocationHandler providerInvocationHandler = createHandler(providerUrl);
        networkTransmissionFactory = createEndpointFactory(providerUrl);
        server = networkTransmissionFactory.createServer(providerUrl, providerInvocationHandler);
    }

    private ServerInvocationHandler createHandler(Url providerUrl) {
        String address = providerUrl.getAddress();
        ServerInvocationHandler providerInvocationHandler = ADDRESS_2_PROVIDER_INVOCATION_HANDLER.get(address);
        if (providerInvocationHandler == null) {
            ProtectedServerInvocationHandler handler = new ProtectedServerInvocationHandler();
//            StatsUtil.registryStatisticCallback(router);
            ADDRESS_2_PROVIDER_INVOCATION_HANDLER.putIfAbsent(address, handler);
            providerInvocationHandler = ADDRESS_2_PROVIDER_INVOCATION_HANDLER.get(address);
        }
        providerInvocationHandler.addProvider(providerUrl);
        return providerInvocationHandler;
    }

    private NetworkTransmissionFactory createEndpointFactory(Url providerUrl) {
        String endpointFactoryName = providerUrl.getOption(NETWORK_TRANSMISSION, NETWORK_TRANSMISSION_VAL_NETTY);
        return NetworkTransmissionFactory.getInstance(endpointFactoryName);
    }

    @Override
    protected boolean doExpose() {
        return server.open();
    }

    @Override
    public boolean isActive() {
        return server.isActive();
    }

    @Override
    public void cancelExpose() {
        ServerInvocationHandler handler = ADDRESS_2_PROVIDER_INVOCATION_HANDLER.get(providerUrl.getAddress());
        if (handler != null) {
            handler.removeProvider(providerUrl);
        }
        log.info("Cancelled exposed provider url: [{}]", providerUrl);
    }

    @Override
    public void destroy() {
        networkTransmissionFactory.destroyServer(server, providerUrl);
        log.info("Destroyed provider url: [{}]", providerUrl);
    }
}
