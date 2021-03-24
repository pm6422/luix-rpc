package org.infinity.rpc.core.config;

import lombok.Data;

@Data
public class ProviderConfig extends ServiceConfig {
    public static final String  PREFIX     = "provider";
    /**
     * Indicates whether all the providers needs to be exposed
     */
    private             boolean exposed    = true;
    /**
     * Indicates whether all the providers were exposed automatically
     */
    private             boolean autoExpose = true;

    public void init() {
    }

    @Override
    public void checkIntegrity() {

    }

    @Override
    public void checkValidity() {

    }
}
