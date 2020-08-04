package org.infinity.rpc.core.protocol.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.exchange.request.impl.DefaultRequester;
import org.infinity.rpc.core.protocol.AbstractProtocol;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.annotation.NameAs;

@NameAs("infinity")
@Slf4j
public class InfinityProtocol extends AbstractProtocol {
    @Override
    protected <T> Requester<T> doCreate(Class<T> interfaceClass, Url providerUrl) {
        return new DefaultRequester<>(interfaceClass, providerUrl);
    }
}
