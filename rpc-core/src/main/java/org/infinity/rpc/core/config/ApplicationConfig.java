package org.infinity.rpc.core.config;

import lombok.Data;
import org.infinity.rpc.core.utils.DebugModeHolder;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
public class ApplicationConfig {
    public static final String  PREFIX    = "application";
    /**
     * Application name
     * Keep unique
     */
    @NotEmpty
    private             String  name;
    /**
     * Application description
     */
    @Size(max = 20)
    private             String  description;
    /**
     * Team name
     */
    @NotEmpty
    private             String  team;
    /**
     * Owner mail
     */
    @NotEmpty
    private             String  ownerMail;
    /**
     * Environment variable, e.g. dev, test or prod
     */
    private             String  env;
    /**
     * Debug mode
     */
    private             boolean debugMode = false;

    public void init() {
        checkIntegrity();
        checkValidity();
        // Set debug mode
        DebugModeHolder.setDebugMode(debugMode);
    }

    private void checkIntegrity() {
    }

    private void checkValidity() {
    }
}