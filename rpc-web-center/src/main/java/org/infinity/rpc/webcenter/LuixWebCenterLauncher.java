package org.infinity.rpc.webcenter;

import org.infinity.rpc.spring.boot.EnableRpc;
import org.infinity.rpc.webcenter.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableRpc
@EnableConfigurationProperties({ApplicationProperties.class})
public class LuixWebCenterLauncher {
    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LuixWebCenterLauncher.class, args);
    }
}
