package com.luixtech.rpc.democlient.config;

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
    private final HttpClient httpClient = new HttpClient();
    private final Url        url        = new Url();

    @Data
    public static class HttpClient {
        private int readTimeout;
        private int retryCount;
    }

    @Data
    public static class Url {
        private String appServiceProviderUrl;
    }
}
