package org.infinity.luix.webcenter;

import org.infinity.luix.spring.boot.EnableLuixRpc;
import org.infinity.luix.webcenter.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableLuixRpc
@EnableConfigurationProperties({ApplicationProperties.class})
public class LuixWebCenterLauncher {
    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        fixEmbedDbIssue();
        SpringApplication.run(LuixWebCenterLauncher.class, args);
    }

    private static void fixEmbedDbIssue() {
        if ("aarch64".equals(System.getProperty("os.arch"))) {
            // Resolve flapdoodle embed db compatibility issue on M1 CPU
            // Refer: https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/337
            System.setProperty("os.arch", "x86_64");
        }
    }
}
