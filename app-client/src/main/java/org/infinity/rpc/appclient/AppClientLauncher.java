package org.infinity.rpc.appclient;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.infinity.rpc.appclient.config.ApplicationConstants;
import org.infinity.rpc.appclient.utils.NetworkIpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;

@SpringBootApplication
public class AppClientLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppClientLauncher.class);

    @Autowired
    private Environment env;

    /**
     * Entrance method which used to run the application. Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        SpringApplication app = new SpringApplication(AppClientLauncher.class);
        Environment env = app.run(args).getEnvironment();
        printAppInfo(env);
    }

    private static void printAppInfo(Environment env) throws IOException {
        String appBanner = StreamUtils.copyToString(new ClassPathResource("config/banner-app.txt").getInputStream(),
                Charset.defaultCharset());
        LOGGER.info(appBanner, env.getProperty("spring.application.name"),
                StringUtils.isEmpty(env.getProperty("server.ssl.key-store")) ? "http" : "https",
                NetworkIpUtils.INTRANET_IP,
                env.getProperty("server.port"),
                StringUtils.defaultString(env.getProperty("server.servlet.context-path")),
                StringUtils.isEmpty(env.getProperty("server.ssl.key-store")) ? "http" : "https",
                NetworkIpUtils.INTERNET_IP,
                env.getProperty("server.port"),
                StringUtils.defaultString(env.getProperty("server.servlet.context-path")),
                org.springframework.util.StringUtils.arrayToCommaDelimitedString(env.getActiveProfiles()),
                env.getProperty("PID"),
                Charset.defaultCharset(),
                env.getProperty("LOG_PATH") + "-" + DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(new Date()) + ".log");
    }

    @PostConstruct
    private void validateProfiles() {
        Assert.notEmpty(env.getActiveProfiles(), "No Spring profile configured.");
        Arrays.asList(env.getActiveProfiles()).stream()
                .filter(activeProfile -> !ArrayUtils.contains(ApplicationConstants.AVAILABLE_PROFILES, activeProfile))
                .findFirst().ifPresent((activeProfile) -> {
            LOGGER.error("Misconfigured application with an illegal profile '{}'!", activeProfile);
            System.exit(0);
        });
    }
}
