package org.infinity.luix.core.protocol.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.server.exposer.Exposable;
import org.infinity.luix.core.server.exposer.impl.JvmExposer;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.protocol.AbstractProtocol;
import org.infinity.luix.utilities.serviceloader.annotation.SpiName;

@SpiName(ProtocolConstants.PROTOCOL_VAL_JVM)
@Slf4j
public class JvmProtocol extends AbstractProtocol {
    @Override
    protected Exposable doExpose(Url providerUrl) {
        return new JvmExposer(providerUrl, this.exposedProviders);
    }
}
