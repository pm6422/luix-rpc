/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package org.infinity.rpc.core.config.spring.server.exporter.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.server.exporter.AbstractExporter;
import org.infinity.rpc.core.config.spring.server.exporter.Exportable;
import org.infinity.rpc.core.config.spring.server.messagehandler.impl.ProviderMessageRouter;
import org.infinity.rpc.core.config.spring.server.messagehandler.impl.ProviderProtectedMessageRouter;
import org.infinity.rpc.core.config.spring.server.stub.ProviderStub;
import org.infinity.rpc.core.exchange.transport.endpoint.EndpointFactory;
import org.infinity.rpc.core.exchange.transport.server.Server;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.util.Map;

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
        String endpointFactoryName = providerStub.getUrl().getParameter(Url.PARAM_ENDPOINT_FACTORY, Url.PARAM_ENDPOINT_FACTORY_DEFAULT_VALUE);
        endpointFactory = EndpointFactory.getInstance(endpointFactoryName);
        server = endpointFactory.createServer(providerStub.getUrl(), requestRouter);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void cancelExport() {
        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerStub.getUrl());
        String ipPort = providerStub.getUrl().getServerPortStr();

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
        String ipPort = url.getServerPortStr();
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
