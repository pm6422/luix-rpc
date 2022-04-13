package com.luixtech.rpc.webcenter;

import com.luixtech.rpc.spring.boot.starter.EnableLuixRpc;
import com.luixtech.rpc.webcenter.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableLuixRpc
@EnableConfigurationProperties({ApplicationProperties.class})
public class LuixRpcWebCenterLauncher {
    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LuixRpcWebCenterLauncher.class, args);
    }
}
