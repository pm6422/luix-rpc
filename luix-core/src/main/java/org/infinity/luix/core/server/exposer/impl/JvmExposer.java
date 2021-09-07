package org.infinity.luix.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;
import org.infinity.luix.core.server.exposer.AbstractExposer;
import org.infinity.luix.core.server.exposer.Exposable;

import java.util.Map;

@Slf4j
public class JvmExposer extends AbstractExposer {

    protected final Map<String, Exposable> exposedProviders;

    public JvmExposer(Url providerUrl, Map<String, Exposable> exposedProviders) {
        super(providerUrl);
        this.exposedProviders = exposedProviders;
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
    public void cancelExpose() {
        String protocolKey = RpcFrameworkUtils.getProtocolKey(providerUrl);
        Exposable exporter = exposedProviders.remove(protocolKey);
        if (exporter != null) {
            exporter.destroy();
        }
        log.info("Undone exposed url [{}]", providerUrl);
    }

    @Override
    public void destroy() {
        // Leave blank intentionally
    }
}
