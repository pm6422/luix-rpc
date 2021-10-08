package org.infinity.luix.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.server.exposer.AbstractProviderExposer;
import org.infinity.luix.core.url.Url;

@Slf4j
public class JvmProviderExposer extends AbstractProviderExposer {

    public JvmProviderExposer(Url providerUrl) {
        super(providerUrl);
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
        log.info("Cancelled exposed provider url: [{}]", providerUrl);
    }

    @Override
    public void destroy() {
        // Leave blank intentionally
    }
}
