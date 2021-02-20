package org.infinity.rpc.core.server.exporter.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.server.exporter.AbstractExporter;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.server.messagehandler.impl.ProviderMessageRouter;
import org.infinity.rpc.core.server.messagehandler.impl.ProviderProtectedMessageRouter;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.exchange.endpoint.EndpointFactory;
import org.infinity.rpc.core.exchange.server.Server;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.util.Map;

import static org.infinity.rpc.core.constant.ProtocolConstants.ENDPOINT_FACTORY;
import static org.infinity.rpc.core.constant.ProtocolConstants.ENDPOINT_FACTORY_DEFAULT_VALUE;

@Slf4j
public class DefaultRpcExporter<T> extends AbstractExporter<T> {

    protected final Map<String, ProviderMessageRouter> ipPort2RequestRouter;
    protected final Map<String, Exportable<?>>         exporterMap;
    protected       Server                             server;
    protected       EndpointFactory                    endpointFactory;

    public DefaultRpcExporter(ProviderStub<T> providerStub,
                              Map<String, ProviderMessageRouter> ipPort2RequestRouter,
                              Map<String, Exportable<?>> exporterMap) {
        super(providerStub);
        this.exporterMap = exporterMap;
        this.ipPort2RequestRouter = ipPort2RequestRouter;

        ProviderMessageRouter requestRouter = initRequestRouter(providerStub.getUrl());
        String endpointFactoryName = providerStub.getUrl().getOption(ENDPOINT_FACTORY, ENDPOINT_FACTORY_DEFAULT_VALUE);
        endpointFactory = EndpointFactory.getInstance(endpointFactoryName);
        server = endpointFactory.createServer(providerStub.getUrl(), requestRouter);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void cancelExport() {
        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerStub.getUrl());
        String ipPort = providerStub.getUrl().getAddress();

        Exportable<T> exporter = (Exportable<T>) exporterMap.remove(protocolKey);
        if (exporter != null) {
            exporter.destroy();
        }

        ProviderMessageRouter requestRouter = ipPort2RequestRouter.get(ipPort);
        if (requestRouter != null) {
            requestRouter.removeProvider(providerStub);
        }

        log.info("DefaultRpcExporter unexport Success: url={}", providerStub.getUrl());
    }

    @Override
    protected boolean doInit() {
        return server.open();
    }

    @Override
    public boolean isAvailable() {
        return server.isActive();
    }

    @Override
    public void destroy() {
        endpointFactory.safeReleaseResource(server, providerStub.getUrl());
        log.info("DefaultRpcExporter destroy Success: url={}", providerStub.getUrl());
    }

    protected ProviderMessageRouter initRequestRouter(Url url) {
        String ipPort = url.getAddress();
        ProviderMessageRouter requestRouter = ipPort2RequestRouter.get(ipPort);

        if (requestRouter == null) {
            ProviderProtectedMessageRouter router = new ProviderProtectedMessageRouter();
//            StatsUtil.registryStatisticCallback(router);
            ipPort2RequestRouter.putIfAbsent(ipPort, router);
            requestRouter = ipPort2RequestRouter.get(ipPort);
        }
        requestRouter.addProvider(providerStub);

        return requestRouter;
    }
}
