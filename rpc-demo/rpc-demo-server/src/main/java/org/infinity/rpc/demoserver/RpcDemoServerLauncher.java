package org.infinity.rpc.demoserver;

import org.infinity.rpc.spring.boot.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@EnableRpc
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class RpcDemoServerLauncher {
    public static ConfigurableApplicationContext applicationContext;

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        applicationContext = SpringApplication.run(RpcDemoServerLauncher.class, args);
    }
}
