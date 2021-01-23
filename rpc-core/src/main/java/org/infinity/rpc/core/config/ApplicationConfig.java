package org.infinity.rpc.core.config;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
public class ApplicationConfig {
    /**
     * Application name
     * Keep unique
     */
    @NotEmpty
    private String name;
    /**
     * Application description
     */
    @Size(max = 20)
    private String description;
    /**
     * Team name
     */
    @NotEmpty
    private String team;
    /**
     * Owner mail
     */
    @NotEmpty
    private String ownerMail;
    /**
     * Environment variable, e.g. dev, test or prod
     */
    private String env;

    public void init() {
        checkIntegrity();
        checkValidity();
    }

    private void checkIntegrity() {
    }

    private void checkValidity() {
    }
}