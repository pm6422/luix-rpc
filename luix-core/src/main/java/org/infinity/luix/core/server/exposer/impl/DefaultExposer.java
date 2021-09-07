package org.infinity.luix.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.exchange.server.Server;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;
import org.infinity.luix.core.exchange.endpoint.EndpointFactory;
import org.infinity.luix.core.server.exposer.AbstractExposer;
import org.infinity.luix.core.server.exposer.Exposable;
import org.infinity.luix.core.server.messagehandler.impl.ProviderInvocationHandler;
import org.infinity.luix.core.server.messagehandler.impl.ProviderProtectedInvocationHandler;

import java.util.Map;

@Slf4j
public class DefaultExposer extends AbstractExposer {

    protected final Map<String, ProviderInvocationHandler> ipPort2RequestRouter;
    protected final Map<String, Exposable>                 exposedProviders;
    protected       Server                                 server;
    protected       EndpointFactory         endpointFactory;

    public DefaultExposer(Url providerUrl,
                          Map<String, ProviderInvocationHandler> ipPort2RequestRouter,
                          Map<String, Exposable> exposedProviders) {
        super(providerUrl);
        this.exposedProviders = exposedProviders;
        this.ipPort2RequestRouter = ipPort2RequestRouter;

        ProviderInvocationHandler requestRouter = initRequestRouter(providerUrl);
        String endpointFactoryName = providerUrl.getOption(ProtocolConstants.ENDPOINT_FACTORY,
                ProtocolConstants.ENDPOINT_FACTORY_VAL_NETTY);
        endpointFactory = EndpointFactory.getInstance(endpointFactoryName);
        server = endpointFactory.createServer(providerUrl, requestRouter);
    }

    @Override
    protected boolean doInit() {
        return server.open();
    }

    @Override
    public boolean isActive() {
        return server.isActive();
    }

    @Override
    public void cancelExpose() {
        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerUrl);
        String ipPort = providerUrl.getAddress();

        Exposable exporter = exposedProviders.remove(protocolKey);
        if (exporter != null) {
            exporter.destroy();
        }

        ProviderInvocationHandler requestRouter = ipPort2RequestRouter.get(ipPort);
        if (requestRouter != null) {
            requestRouter.removeProvider(providerUrl);
        }
        log.info("Undone exposed url [{}]", providerUrl);
    }

    @Override
    public void destroy() {
        endpointFactory.safeReleaseResource(server, providerUrl);
        log.info("Destroy Success: url={}", providerUrl);
    }

    protected ProviderInvocationHandler initRequestRouter(Url url) {
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
}
