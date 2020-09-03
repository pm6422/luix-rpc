package org.infinity.rpc.core.protocol.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.request.ProviderRequester;
import org.infinity.rpc.core.exchange.request.impl.DefaultProviderRequester;
import org.infinity.rpc.core.protocol.AbstractProtocol;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

@ServiceName("infinity")
@Slf4j
public class InfinityProtocol extends AbstractProtocol {
    @Override
    protected <T> ProviderRequester<T> doCreate(Class<T> interfaceClass, Url providerUrl) {
        return new DefaultProviderRequester<>(interfaceClass, providerUrl);
    }
}
