package com.luixtech.rpc.core.server.exposer.impl;

import com.luixtech.rpc.core.constant.ProtocolConstants;
import com.luixtech.rpc.core.exchange.endpoint.NetworkTransmissionFactory;
import com.luixtech.rpc.core.exchange.server.Server;
import com.luixtech.rpc.core.server.exposer.AbstractProviderExposer;
import com.luixtech.rpc.core.server.handler.impl.ProtectedServerInvocationHandler;
import com.luixtech.rpc.core.server.handler.impl.ServerInvocationHandler;
import com.luixtech.rpc.core.url.Url;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ServerProviderExposer extends AbstractProviderExposer {

    /**
     * 多个服务可在不同端口进行服务暴露
     */
    protected static final Map<String, ServerInvocationHandler> ADDRESS_2_PROVIDER_INVOCATION_HANDLER = new ConcurrentHashMap<>();
    protected              Server                               server;
    protected              NetworkTransmissionFactory           networkTransmissionFactory;

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
        String endpointFactoryName = providerUrl.getOption(ProtocolConstants.NETWORK_TRANSMISSION, ProtocolConstants.NETWORK_TRANSMISSION_VAL_NETTY);
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
