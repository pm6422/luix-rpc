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
import org.infinity.rpc.core.config.spring.server.providerwrapper.ProviderWrapper;
import org.infinity.rpc.core.exchange.transport.endpoint.EndpointFactory;
import org.infinity.rpc.core.exchange.transport.server.Server;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;
import org.infinity.rpc.utilities.spi.ServiceLoader;

import java.util.Map;

@Slf4j
public class DefaultRpcExporter<T> extends AbstractExporter<T> {

    protected final Map<String, ProviderMessageRouter> ipPort2RequestRouter;
    protected final Map<String, Exportable<?>>         exporterMap;
    protected       Server                             server;
    protected       EndpointFactory                    endpointFactory;

    public DefaultRpcExporter(ProviderWrapper<T> providerWrapper,
                              Map<String, ProviderMessageRouter> ipPort2RequestRouter,
                              Map<String, Exportable<?>> exporterMap) {
        super(providerWrapper);
        this.exporterMap = exporterMap;
        this.ipPort2RequestRouter = ipPort2RequestRouter;

        ProviderMessageRouter requestRouter = initRequestRouter(providerWrapper.getUrl());
        String endpointFactoryName = providerWrapper.getUrl().getParameter(Url.PARAM_ENDPOINT_FACTORY, Url.PARAM_ENDPOINT_FACTORY_DEFAULT_VALUE);
        endpointFactory = EndpointFactory.getInstance(endpointFactoryName);
        server = endpointFactory.createServer(providerWrapper.getUrl(), requestRouter);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void unexport() {
        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerWrapper.getUrl());
        String ipPort = providerWrapper.getUrl().getServerPortStr();

        Exportable<T> exporter = (Exportable<T>) exporterMap.remove(protocolKey);
        if (exporter != null) {
            exporter.destroy();
        }

        ProviderMessageRouter requestRouter = ipPort2RequestRouter.get(ipPort);
        if (requestRouter != null) {
            requestRouter.removeProvider(providerWrapper);
        }

        log.info("DefaultRpcExporter unexport Success: url={}", providerWrapper.getUrl());
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
        endpointFactory.safeReleaseResource(server, providerWrapper.getUrl());
        log.info("DefaultRpcExporter destroy Success: url={}", providerWrapper.getUrl());
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
        requestRouter.addProvider(providerWrapper);

        return requestRouter;
    }
}
