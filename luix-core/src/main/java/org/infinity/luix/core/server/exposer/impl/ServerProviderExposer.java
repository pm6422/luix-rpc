package org.infinity.luix.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.exchange.endpoint.EndpointFactory;
import org.infinity.luix.core.exchange.server.Server;
import org.infinity.luix.core.server.exposer.AbstractProviderExposer;
import org.infinity.luix.core.server.messagehandler.impl.ProviderInvocationHandler;
import org.infinity.luix.core.server.messagehandler.impl.ProviderProtectedInvocationHandler;
import org.infinity.luix.core.url.Url;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ServerProviderExposer extends AbstractProviderExposer {

    /**
     * 多个service可能在相同端口进行服务暴露，因此来自同个端口的请求需要进行路由以找到相应的服务，同时不在该端口暴露的服务不应该被找到
     */
    protected static final Map<String, ProviderInvocationHandler> IP_PORT_2_REQUEST_ROUTER = new ConcurrentHashMap<>();
    protected              Server                                 server;
    protected              EndpointFactory                        endpointFactory;

    public ServerProviderExposer(Url providerUrl) {
        super(providerUrl);

        ProviderInvocationHandler providerInvocationHandler = initRequestRouter(providerUrl);
        String endpointFactoryName = providerUrl.getOption(ProtocolConstants.ENDPOINT_FACTORY,
                ProtocolConstants.ENDPOINT_FACTORY_VAL_NETTY);
        endpointFactory = EndpointFactory.getInstance(endpointFactoryName);
        server = endpointFactory.createServer(providerUrl, providerInvocationHandler);
    }

    private ProviderInvocationHandler initRequestRouter(Url url) {
        String ipPort = url.getAddress();
        ProviderInvocationHandler requestRouter = IP_PORT_2_REQUEST_ROUTER.get(ipPort);
        if (requestRouter == null) {
            ProviderProtectedInvocationHandler router = new ProviderProtectedInvocationHandler();
//            StatsUtil.registryStatisticCallback(router);
            IP_PORT_2_REQUEST_ROUTER.putIfAbsent(ipPort, router);
            requestRouter = IP_PORT_2_REQUEST_ROUTER.get(ipPort);
        }
        requestRouter.addProvider(providerUrl);
        return requestRouter;
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
        String ipPort = providerUrl.getAddress();
        ProviderInvocationHandler requestRouter = IP_PORT_2_REQUEST_ROUTER.get(ipPort);
        if (requestRouter != null) {
            requestRouter.removeProvider(providerUrl);
        }
        log.info("Cancelled exposed provider url: [{}]", providerUrl);
    }

    @Override
    public void destroy() {
        endpointFactory.safeReleaseResource(server, providerUrl);
        log.info("Destroy Success: url={}", providerUrl);
    }
}
