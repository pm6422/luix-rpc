package org.infinity.rpc.core.protocol.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.protocol.AbstractProtocol;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.server.exporter.impl.JvmRpcExporter;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import static org.infinity.rpc.core.constant.ProtocolConstants.PROTOCOL_VAL_JVM;

@SpiName(PROTOCOL_VAL_JVM)
@Slf4j
public class JvmProtocol extends AbstractProtocol {
    @Override
    protected Exportable doExport(Url providerUrl) {
        return new JvmRpcExporter(providerUrl, this.exporterMap);
    }
}
