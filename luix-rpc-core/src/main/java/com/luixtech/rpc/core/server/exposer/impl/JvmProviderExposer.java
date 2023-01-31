package com.luixtech.rpc.core.server.exposer.impl;

import com.luixtech.rpc.core.server.exposer.AbstractProviderExposer;
import com.luixtech.rpc.core.url.Url;
import lombok.extern.slf4j.Slf4j;

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
