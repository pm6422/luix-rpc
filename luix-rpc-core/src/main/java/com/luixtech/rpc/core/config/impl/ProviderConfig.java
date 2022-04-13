package com.luixtech.rpc.core.config.impl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
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
        log.info("Luix provider configuration: {}", this);
    }

    @Override
    public void checkIntegrity() {

    }

    @Override
    public void checkValidity() {

    }
}
