package org.infinity.luix.core.server.exporter.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.exchange.server.Server;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;
import org.infinity.luix.core.exchange.endpoint.EndpointFactory;
import org.infinity.luix.core.server.exporter.AbstractExporter;
import org.infinity.luix.core.server.exporter.Exportable;
import org.infinity.luix.core.server.messagehandler.impl.ProviderHandler;
import org.infinity.luix.core.server.messagehandler.impl.ProviderProtectedHandler;

import java.util.Map;

@Slf4j
public class DefaultRpcExporter extends AbstractExporter {

    protected final Map<String, ProviderHandler> ipPort2RequestRouter;
    protected final Map<String, Exportable> exporterMap;
    protected       Server                  server;
    protected       EndpointFactory         endpointFactory;

    public DefaultRpcExporter(Url providerUrl,
                              Map<String, ProviderHandler> ipPort2RequestRouter,
                              Map<String, Exportable> exporterMap) {
        super(providerUrl);
        this.exporterMap = exporterMap;
        this.ipPort2RequestRouter = ipPort2RequestRouter;

        ProviderHandler requestRouter = initRequestRouter(providerUrl);
        String endpointFactoryName = providerUrl.getOption(ProtocolConstants.ENDPOINT_FACTORY, ProtocolConstants.ENDPOINT_FACTORY_VAL_NETTY);
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
    public void cancelExport() {
        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerUrl);
        String ipPort = providerUrl.getAddress();

        Exportable exporter = exporterMap.remove(protocolKey);
        if (exporter != null) {
            exporter.destroy();
        }

        ProviderHandler requestRouter = ipPort2RequestRouter.get(ipPort);
        if (requestRouter != null) {
            requestRouter.removeProvider(providerUrl);
        }
        log.info("Undone exported url [{}]", providerUrl);
    }

    @Override
    public void destroy() {
        endpointFactory.safeReleaseResource(server, providerUrl);
        log.info("DefaultRpcExporter destroy Success: url={}", providerUrl);
    }

    protected ProviderHandler initRequestRouter(Url url) {
        String ipPort = url.getAddress();
        ProviderHandler requestRouter = ipPort2RequestRouter.get(ipPort);

        if (requestRouter == null) {
            ProviderProtectedHandler router = new ProviderProtectedHandler();
//            StatsUtil.registryStatisticCallback(router);
            ipPort2RequestRouter.putIfAbsent(ipPort, router);
            requestRouter = ipPort2RequestRouter.get(ipPort);
        }
        requestRouter.addProvider(providerUrl);

        return requestRouter;
    }
}
