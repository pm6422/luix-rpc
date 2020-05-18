package org.infinity.rpc.webcenter.config;

import lombok.Data;
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
@Data
public class ApplicationProperties {
    private final Http http = new Http();

    @Data
    public static class Http {
        private final Cache cache = new Cache();

        @Data
        public static class Cache {
            private Long timeToLiveInDays = 31L;
        }
    }
}
