package com.luixtech.rpc.webcenter.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.cors.CorsConfiguration;

import javax.validation.constraints.NotNull;

/**
 * Properties specific to Application.
 *
 * <p>
 * Properties are configured in the application.yml file.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
@Validated
@Getter
public class ApplicationProperties {
    private final CorsConfiguration cors           = new CorsConfiguration();
    private final UserEventAudit    userEventAudit = new UserEventAudit();
    private final Account           account        = new Account();
    private final Security          security       = new Security();

    @Data
    public static class UserEventAudit {
        private boolean enabled;
    }

    @Data
    public static class Account {
        private String defaultPassword;
    }

    @Data
    public static class Security {
        private String         contentSecurityPolicy;
        private Authentication authentication = new Authentication();

        @Data
        public static class Authentication {
            private Jwt jwt = new Jwt();

            @Data
            public static class Jwt {
                @NotNull
                private String base64Secret;
                private long   tokenValidityInSeconds              = 1800; // 30 minutes
                private long   tokenValidityInSecondsForRememberMe = 2592000; // 30 days
            }
        }
    }
}
