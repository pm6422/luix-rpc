package org.infinity.rpc.core.server.exporter.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.server.exporter.AbstractExporter;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.util.Map;

@Slf4j
public class JvmRpcExporter extends AbstractExporter {

    protected final Map<String, Exportable> exporterMap;

    public JvmRpcExporter(Url providerUrl, Map<String, Exportable> exporterMap) {
        super(providerUrl);
        this.exporterMap = exporterMap;
    }

    @Override
    protected boolean doInit() {
        return true;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void cancelExport() {
        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerUrl);
        Exportable exporter = exporterMap.remove(protocolKey);
        if (exporter != null) {
            exporter.destroy();
        }
        log.info("Undone exported url [{}]", providerUrl);
    }

    @Override
    public void destroy() {
        // Leave blank intentionally
    }
}
