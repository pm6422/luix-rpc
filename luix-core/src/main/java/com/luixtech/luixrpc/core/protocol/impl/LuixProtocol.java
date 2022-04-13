package com.luixtech.luixrpc.core.protocol.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.constant.ProtocolConstants;
import com.luixtech.luixrpc.core.protocol.AbstractProtocol;
import com.luixtech.luixrpc.core.server.exposer.ProviderExposable;
import com.luixtech.luixrpc.core.server.exposer.impl.ServerProviderExposer;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.utilities.serviceloader.annotation.SpiName;

@SpiName(ProtocolConstants.PROTOCOL_VAL_LUIX)
@Slf4j
public class LuixProtocol extends AbstractProtocol {

    @Override
    protected ProviderExposable createExposer(Url providerUrl) {
        return new ServerProviderExposer(providerUrl);
    }
}
