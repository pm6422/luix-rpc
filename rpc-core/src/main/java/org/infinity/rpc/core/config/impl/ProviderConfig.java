package org.infinity.rpc.core.config.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ProviderConfig extends ServiceConfig {
    public static final String  PREFIX     = "provider";
    /**
     * Health checker
     */
    private             String  healthChecker;
    /**
     * Indicates whether all the providers were exposed to registry automatically
     */
    private             boolean autoExpose = true;

    public void init() {
        log.info("Infinity RPC provider configuration: {}", this);
    }

    @Override
    public void checkIntegrity() {

    }

    @Override
    public void checkValidity() {

    }
}
