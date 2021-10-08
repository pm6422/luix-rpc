package org.infinity.luix.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.exchange.endpoint.EndpointFactory;
import org.infinity.luix.core.exchange.server.Server;
import org.infinity.luix.core.server.exposer.AbstractProviderExposer;
import org.infinity.luix.core.server.messagehandler.impl.ProviderInvocationHandler;
import org.infinity.luix.core.server.messagehandler.impl.ProviderProtectedInvocationHandler;
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
    protected static final Map<String, ProviderInvocationHandler> ADDRESS_2_PROVIDER_INVOCATION_HANDLER = new ConcurrentHashMap<>();
    protected              Server                                 server;
    protected              EndpointFactory                        endpointFactory;

    public ServerProviderExposer(Url providerUrl) {
        super(providerUrl);

        ProviderInvocationHandler providerInvocationHandler = createHandler(providerUrl);
        String endpointFactoryName = providerUrl.getOption(ENDPOINT_FACTORY, ENDPOINT_FACTORY_VAL_NETTY);
        endpointFactory = EndpointFactory.getInstance(endpointFactoryName);
        server = endpointFactory.createServer(providerUrl, providerInvocationHandler);
    }

    private ProviderInvocationHandler createHandler(Url providerUrl) {
        String address = providerUrl.getAddress();
        ProviderInvocationHandler providerInvocationHandler = ADDRESS_2_PROVIDER_INVOCATION_HANDLER.get(address);
        if (providerInvocationHandler == null) {
            ProviderProtectedInvocationHandler handler = new ProviderProtectedInvocationHandler();
//            StatsUtil.registryStatisticCallback(router);
            ADDRESS_2_PROVIDER_INVOCATION_HANDLER.putIfAbsent(address, handler);
            providerInvocationHandler = ADDRESS_2_PROVIDER_INVOCATION_HANDLER.get(address);
        }
        providerInvocationHandler.addProvider(providerUrl);
        return providerInvocationHandler;
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
        ProviderInvocationHandler handler = ADDRESS_2_PROVIDER_INVOCATION_HANDLER.get(providerUrl.getAddress());
        if (handler != null) {
            handler.removeProvider(providerUrl);
        }
        log.info("Cancelled exposed provider url: [{}]", providerUrl);
    }

    @Override
    public void destroy() {
        endpointFactory.safeReleaseResource(server, providerUrl);
        log.info("Destroyed provider url: [{}]", providerUrl);
    }
}
