package org.infinity.luix.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;
import org.infinity.luix.core.server.exposer.AbstractProviderExposer;
import org.infinity.luix.core.server.exposer.ProviderExposable;

import java.util.Map;

@Slf4j
public class JvmProviderExposer extends AbstractProviderExposer {

    protected final Map<String, ProviderExposable> exposedProviders;

    public JvmProviderExposer(Url providerUrl, Map<String, ProviderExposable> exposedProviders) {
        super(providerUrl);
        this.exposedProviders = exposedProviders;
    }

    @Override
    protected boolean doExpose() {
        return true;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void cancelExpose() {
        String providerKey = RpcFrameworkUtils.getProviderKey(providerUrl);
        ProviderExposable exporter = exposedProviders.remove(providerKey);
        if (exporter != null) {
            exporter.destroy();
        }
        log.info("Cancelled exposed provider url: [{}]", providerUrl);
    }

    @Override
    public void destroy() {
        // Leave blank intentionally
    }
}
