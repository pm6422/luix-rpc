package com.luixtech.rpc.portal;

import com.luixtech.framework.EnableLuixFramework;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableLuixFramework
public class LuixRpcPortalApplication {

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LuixRpcPortalApplication.class, args);
    }
}
