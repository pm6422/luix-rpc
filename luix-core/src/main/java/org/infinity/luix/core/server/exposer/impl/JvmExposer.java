package org.infinity.luix.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;
import org.infinity.luix.core.server.exposer.AbstractExposer;
import org.infinity.luix.core.server.exposer.Exposable;

import java.util.Map;

@Slf4j
public class JvmExposer extends AbstractExposer {

    protected final Map<String, Exposable> exporterMap;

    public JvmExposer(Url providerUrl, Map<String, Exposable> exporterMap) {
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
        Exposable exporter = exporterMap.remove(protocolKey);
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
