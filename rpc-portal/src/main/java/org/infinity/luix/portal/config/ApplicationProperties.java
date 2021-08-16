package org.infinity.luix.portal.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

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
    private final Http   http   = new Http();
    private final Ribbon ribbon = new Ribbon();

    @Data
    public static class Http {
        private final Cache cache = new Cache();

        @Data
        public static class Cache {
            /**
             * Expired days
             */
            private Long expiredAfter = 31L;
        }
    }

    @Data
    public static class Ribbon {
        private String[] displayOnActiveProfiles;
    }
}
