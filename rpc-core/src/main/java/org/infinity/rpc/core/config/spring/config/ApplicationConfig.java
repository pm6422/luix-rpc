package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Data
@Validated
public class ApplicationConfig {
    /**
     * Application ID
     * It can guarantee the unique value under a cluster
     */
    private String id = "ID-" + IdGenerator.generateShortId();
    /**
     * Application name
     * It was set by user, so the name maybe duplicated
     */
    @NotNull
    private String name;
    /**
     * Application description
     */
    private String description;
    /**
     * Team name
     */
    private String team;
    /**
     * Owner name
     */
    private String owner;
    /**
     * Owner mail
     */
    private String ownerMail;
    /**
     * Environment variable, e.g. dev, test or prod
     */
    private String env;

    public void initialize() {
        checkIntegrity();
        checkValidity();
    }

    private void checkIntegrity() {
        Validate.notNull(name, "Application name must NOT be null! Please check your configuration.");
    }

    private void checkValidity() {
    }

    public App toApp() {
        App app = new App();
        BeanUtils.copyProperties(this, app);
        return app;
    }
}