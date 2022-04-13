package com.luixtech.luixrpc.core.server.exposer.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.server.exposer.AbstractProviderExposer;
import com.luixtech.luixrpc.core.url.Url;

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
