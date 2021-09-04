package org.infinity.luix.core.protocol.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.server.exporter.Exportable;
import org.infinity.luix.core.server.exporter.impl.JvmRpcExporter;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.protocol.AbstractProtocol;
import org.infinity.luix.utilities.serviceloader.annotation.SpiName;

@SpiName(ProtocolConstants.PROTOCOL_VAL_JVM)
@Slf4j
public class JvmProtocol extends AbstractProtocol {
    @Override
    protected Exportable doExport(Url providerUrl) {
        return new JvmRpcExporter(providerUrl, this.exporterMap);
    }
}
