package org.infinity.rpc.core.config.impl;

import lombok.Data;

@Data
public class ProviderConfig extends ServiceConfig {
    // Build-in methods
    public static final String  METHOD_HEALTH           = "$health";
    public static final String  METHOD_META             = "$methodMeta";
    public static final String  METHOD_APPLICATION_META = "$applicationMeta";
    public static final String  METHOD_SYSTEM_TIME      = "$systemTime";
    public static final String  PREFIX                  = "provider";
    /**
     * Indicates whether all the providers were exposed to registry automatically
     */
    private             boolean autoExpose              = true;

    public void init() {
    }

    @Override
    public void checkIntegrity() {

    }

    @Override
    public void checkValidity() {

    }
}
