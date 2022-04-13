package com.luixtech.rpc.core.protocol.impl;

import com.luixtech.rpc.core.constant.ProtocolConstants;
import com.luixtech.rpc.core.protocol.AbstractProtocol;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.rpc.core.server.exposer.ProviderExposable;
import com.luixtech.rpc.core.server.exposer.impl.JvmProviderExposer;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.utilities.serviceloader.annotation.SpiName;

@SpiName(ProtocolConstants.PROTOCOL_VAL_JVM)
@Slf4j
public class JvmProtocol extends AbstractProtocol {
    @Override
    protected ProviderExposable createExposer(Url providerUrl) {
        return new JvmProviderExposer(providerUrl);
    }
}
