package org.infinity.rpc.appclient;

import org.apache.commons.lang3.ArrayUtils;
import org.infinity.rpc.appclient.config.ApplicationConstants;
import org.infinity.rpc.core.config.spring.annotation.EnableRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;

@EnableRpc
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class AppClientLauncher implements ApplicationContextAware {

    private static final Logger                         LOGGER = LoggerFactory.getLogger(AppClientLauncher.class);
    @Autowired
    private              Environment                    env;
    private static       ConfigurableApplicationContext applicationContext;

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AppClientLauncher.class);
        app.run(args);
    }

    @PostConstruct
    private void validateProfiles() {
        Assert.notEmpty(env.getActiveProfiles(), "No Spring profile configured.");
        Assert.isTrue(env.getActiveProfiles().length == 1, "Multiple profiles are not allowed!");
        Arrays.asList(env.getActiveProfiles()).stream()
                .filter(activeProfile -> !ArrayUtils.contains(ApplicationConstants.AVAILABLE_PROFILES, activeProfile))
                .findFirst().ifPresent((activeProfile) -> {
            LOGGER.error("Misconfigured application with an illegal profile '{}'!", activeProfile);
            System.exit(0);
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }
}
