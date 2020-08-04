package org.infinity.rpc.core.config.spring.config;

import lombok.Data;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.springframework.beans.BeanUtils;

@Data
public class ApplicationConfig {
    // Application ID
    private String id = "ID-" + IdGenerator.generateShortId();
    // Application name
    private String name;
    // Application description
    private String description;
    // Responsible team
    private String team;
    // Application owner
    private String owner;
    // Environment variable, e.g. dev, test or prod
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