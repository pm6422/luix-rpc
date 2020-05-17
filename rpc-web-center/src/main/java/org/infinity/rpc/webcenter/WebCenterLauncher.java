package org.infinity.rpc.webcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

import java.io.IOException;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class WebCenterLauncher {
    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws Exception {
        SpringApplication app = new SpringApplication(WebCenterLauncher.class);
        app.run(args);
    }
}
