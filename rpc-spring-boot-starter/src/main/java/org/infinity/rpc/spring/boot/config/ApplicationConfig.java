package org.infinity.rpc.spring.boot.config;

import lombok.Data;
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Data
@Validated
public class ApplicationConfig {
    /**
     * Application ID
     * It can guarantee the unique value under a cluster
     */
    @NotEmpty
    private String id = "ID-" + IdGenerator.generateShortId();
    /**
     * Application name
     * It was set by user, so the name maybe duplicated
     */
    @NotEmpty
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

    public void init() {
        checkIntegrity();
        checkValidity();
    }

    private void checkIntegrity() {
    }

    private void checkValidity() {
    }

    public App toApp() {
        App app = new App();
        BeanUtils.copyProperties(this, app);
        return app;
    }
}