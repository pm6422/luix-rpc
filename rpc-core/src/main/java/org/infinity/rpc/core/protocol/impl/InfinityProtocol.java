package org.infinity.rpc.core.protocol.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.exchange.request.impl.DefaultProviderCaller;
import org.infinity.rpc.core.protocol.AbstractProtocol;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

@ServiceName("infinity")
@Slf4j
public class InfinityProtocol extends AbstractProtocol {
    @Override
    protected <T> ProviderCaller<T> doCreate(Class<T> interfaceClass, Url providerUrl) {
        return new DefaultProviderCaller<>(interfaceClass, providerUrl);
    }
}
