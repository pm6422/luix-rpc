package org.infinity.luix.core.protocol.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.server.exposer.Exposable;
import org.infinity.luix.core.server.exposer.impl.DefaultExposer;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.protocol.AbstractProtocol;
import org.infinity.luix.core.server.messagehandler.impl.ProviderInvocationHandler;
import org.infinity.luix.utilities.serviceloader.annotation.SpiName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpiName(ProtocolConstants.PROTOCOL_VAL_INFINITY)
@Slf4j
public class InfinityProtocol extends AbstractProtocol {

    /**
     * 多个service可能在相同端口进行服务暴露，因此来自同个端口的请求需要进行路由以找到相应的服务，同时不在该端口暴露的服务不应该被找到
     */
    private final Map<String, ProviderInvocationHandler> ipPort2RequestRouter = new ConcurrentHashMap<>();

    @Override
    protected Exposable doExport(Url providerUrl) {
        return new DefaultExposer(providerUrl, this.ipPort2RequestRouter, this.exporterMap);
    }
}
