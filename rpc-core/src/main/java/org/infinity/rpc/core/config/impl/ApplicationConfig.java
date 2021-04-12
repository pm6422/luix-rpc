package org.infinity.rpc.core.config.impl;

import lombok.Data;
import org.infinity.rpc.core.config.Configurable;
import org.infinity.rpc.core.utils.ApplicationConfigHolder;
import org.infinity.rpc.core.utils.DebugModeHolder;
import org.infinity.rpc.core.utils.JarUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class ApplicationConfig implements Configurable, Serializable {
    private static final long    serialVersionUID = -7916757272373849145L;
    public static final  String  PREFIX           = "application";
    /**
     * Application name
     * Keep unique
     */
    @NotEmpty
    private              String  name;
    /**
     * Application description
     */
    @Size(max = 20)
    private              String  description;
    /**
     * Team name
     */
    @NotEmpty
    private              String  team;
    /**
     * Owner mail
     */
    @NotEmpty
    private              String  ownerMail;
    /**
     * Environment variable, e.g. dev, test or prod
     */
    private              String  env;
    /**
     * Infinity RPC jar version
     */
    private              String  jarVersion       = JarUtils.VERSION;
    /**
     * Debug mode
     */
    private              boolean debugMode        = false;

    public void init() {
        checkIntegrity();
        checkValidity();
        // Set debug mode
        DebugModeHolder.setDebugMode(debugMode);
        ApplicationConfigHolder.set(this);
    }

    @Override
    public void checkIntegrity() {
    }

    @Override
    public void checkValidity() {
    }
}