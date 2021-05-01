package org.infinity.rpc.democlient;

import com.dtflys.forest.springboot.annotation.ForestScan;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.infinity.rpc.democlient.config.ApplicationConstants;
import org.infinity.rpc.spring.boot.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Arrays;


@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@Slf4j
@EnableRpc
@ForestScan(basePackages = "org.infinity.rpc.democlient.restservice")
public class RpcDemoClientLauncher {
    private final Environment env;

    public RpcDemoClientLauncher(Environment env) {
        this.env = env;
    }

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(RpcDemoClientLauncher.class, args);
    }

    @PostConstruct
    private void validateProfiles() {
        Assert.notEmpty(env.getActiveProfiles(), "No Spring profile configured.");
        Assert.isTrue(env.getActiveProfiles().length == 1, "Multiple profiles are not allowed!");
        Arrays.stream(env.getActiveProfiles())
                .filter(activeProfile -> !ArrayUtils.contains(ApplicationConstants.AVAILABLE_PROFILES, activeProfile))
                .findFirst().ifPresent((activeProfile) -> {
            log.error("Mis-configured application with an illegal profile '{}'!", activeProfile);
            System.exit(0);
        });
    }
}
