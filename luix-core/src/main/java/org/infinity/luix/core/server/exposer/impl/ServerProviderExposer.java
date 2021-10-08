package org.infinity.luix.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.exchange.endpoint.EndpointFactory;
import org.infinity.luix.core.exchange.server.Server;
import org.infinity.luix.core.server.exposer.AbstractProviderExposer;
import org.infinity.luix.core.server.exposer.ProviderExposable;
import org.infinity.luix.core.server.messagehandler.impl.ProviderInvocationHandler;
import org.infinity.luix.core.server.messagehandler.impl.ProviderProtectedInvocationHandler;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;

import java.util.Map;

@Slf4j
public class ServerProviderExposer extends AbstractProviderExposer {

    protected final Map<String, ProviderInvocationHandler> ipPort2RequestRouter;
    protected final Map<String, ProviderExposable>         exposedProviders;
    protected       Server                                 server;
    protected       EndpointFactory                        endpointFactory;

    public ServerProviderExposer(Url providerUrl,
                                 Map<String, ProviderInvocationHandler> ipPort2RequestRouter,
                                 Map<String, ProviderExposable> exposedProviders) {
        super(providerUrl);
        this.exposedProviders = exposedProviders;
        this.ipPort2RequestRouter = ipPort2RequestRouter;

        ProviderInvocationHandler providerInvocationHandler = initRequestRouter(providerUrl);
        String endpointFactoryName = providerUrl.getOption(ProtocolConstants.ENDPOINT_FACTORY,
                ProtocolConstants.ENDPOINT_FACTORY_VAL_NETTY);
        endpointFactory = EndpointFactory.getInstance(endpointFactoryName);
        server = endpointFactory.createServer(providerUrl, providerInvocationHandler);
    }

    private ProviderInvocationHandler initRequestRouter(Url url) {
        String ipPort = url.getAddress();
        ProviderInvocationHandler requestRouter = ipPort2RequestRouter.get(ipPort);
        if (requestRouter == null) {
            ProviderProtectedInvocationHandler router = new ProviderProtectedInvocationHandler();
//            StatsUtil.registryStatisticCallback(router);
            ipPort2RequestRouter.putIfAbsent(ipPort, router);
            requestRouter = ipPort2RequestRouter.get(ipPort);
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
        String providerKey = RpcFrameworkUtils.getProviderKey(providerUrl);
        ProviderExposable exporter = exposedProviders.remove(providerKey);
        if (exporter != null) {
            exporter.destroy();
        }

        String ipPort = providerUrl.getAddress();
        ProviderInvocationHandler requestRouter = ipPort2RequestRouter.get(ipPort);
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
