package com.luixtech.rpc.webcenter;

import com.luixtech.rpc.spring.boot.starter.EnableLuixRpc;
import com.luixtech.rpc.webcenter.config.ApplicationProperties;
import com.luixtech.springbootframework.EnableLuixSpringBootFramework;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableLuixRpc
@EnableLuixSpringBootFramework
@EnableConfigurationProperties({ApplicationProperties.class})
public class LuixRpcWebCenterApplication {
    public static final String   DEFAULT_REG         = "consul://localhost:8500/registry";
    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LuixRpcWebCenterApplication.class, args);
    }
}
