package org.infinity.luix.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.exchange.endpoint.EndpointFactory;
import org.infinity.luix.core.exchange.server.Server;
import org.infinity.luix.core.server.exposer.AbstractProviderExposer;
import org.infinity.luix.core.server.messagehandler.impl.ServerInvocationHandler;
import org.infinity.luix.core.server.messagehandler.impl.ServerProtectedInvocationHandler;
import org.infinity.luix.core.url.Url;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.luix.core.constant.ProtocolConstants.ENDPOINT_FACTORY;
import static org.infinity.luix.core.constant.ProtocolConstants.ENDPOINT_FACTORY_VAL_NETTY;

@Slf4j
public class ServerProviderExposer extends AbstractProviderExposer {

    /**
     * 多个服务可在不同端口进行服务暴露
     */
    protected static final Map<String, ServerInvocationHandler> ADDRESS_2_PROVIDER_INVOCATION_HANDLER = new ConcurrentHashMap<>();
    protected              Server                               server;
    protected              EndpointFactory                        endpointFactory;

    public ServerProviderExposer(Url providerUrl) {
        super(providerUrl);

        ServerInvocationHandler providerInvocationHandler = createHandler(providerUrl);
        endpointFactory = createEndpointFactory(providerUrl);
        server = endpointFactory.createServer(providerUrl, providerInvocationHandler);
    }

    private ServerInvocationHandler createHandler(Url providerUrl) {
        String address = providerUrl.getAddress();
        ServerInvocationHandler providerInvocationHandler = ADDRESS_2_PROVIDER_INVOCATION_HANDLER.get(address);
        if (providerInvocationHandler == null) {
            ServerProtectedInvocationHandler handler = new ServerProtectedInvocationHandler();
//            StatsUtil.registryStatisticCallback(router);
            ADDRESS_2_PROVIDER_INVOCATION_HANDLER.putIfAbsent(address, handler);
            providerInvocationHandler = ADDRESS_2_PROVIDER_INVOCATION_HANDLER.get(address);
        }
        providerInvocationHandler.addProvider(providerUrl);
        return providerInvocationHandler;
    }

    private EndpointFactory createEndpointFactory(Url providerUrl) {
        String endpointFactoryName = providerUrl.getOption(ENDPOINT_FACTORY, ENDPOINT_FACTORY_VAL_NETTY);
        return EndpointFactory.getInstance(endpointFactoryName);
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
        endpointFactory.destroyServer(server, providerUrl);
        log.info("Destroyed provider url: [{}]", providerUrl);
    }
}
