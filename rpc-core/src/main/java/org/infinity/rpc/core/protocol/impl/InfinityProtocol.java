package org.infinity.rpc.core.protocol.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.protocol.AbstractProtocol;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.server.exporter.impl.DefaultRpcExporter;
import org.infinity.rpc.core.server.messagehandler.impl.ProviderMessageRouter;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.constant.ProtocolConstants.PROTOCOL_VAL_INFINITY;

@SpiName(PROTOCOL_VAL_INFINITY)
@Slf4j
public class InfinityProtocol<T> extends AbstractProtocol<T> {

    /**
     * 多个service可能在相同端口进行服务暴露，因此来自同个端口的请求需要进行路由以找到相应的服务，同时不在该端口暴露的服务不应该被找到
     */
    private final Map<String, ProviderMessageRouter> ipPort2RequestRouter = new ConcurrentHashMap<>();

    @Override
    protected Exportable<T> createExporter(ProviderStub<T> providerStub) {
        return new DefaultRpcExporter<>(providerStub, this.ipPort2RequestRouter, this.exporterMap);
    }
}
