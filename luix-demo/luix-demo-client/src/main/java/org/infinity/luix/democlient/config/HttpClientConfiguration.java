package org.infinity.luix.democlient.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Configuration
public class HttpClientConfiguration {

    @Resource
    private ApplicationProperties applicationProperties;

    @Bean(destroyMethod = "close")
    public CloseableHttpClient closeableHttpClient() {
        // The HttpClient instance created by PoolingHttpClientConnectionManager can be shared by multiple connections and threads.
        // Instantiate once when the application container is up, and call httpClient.close() when the entire application ends
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        // Connection number of entire pool
        connectionManager.setMaxTotal(40);
        // Connection number per host
        connectionManager.setDefaultMaxPerRoute(20);
        // Set retry handler
        DefaultHttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(
                applicationProperties.getHttpClient().getRetryCount(), true);
        return HttpClientBuilder
                .create()
                .setConnectionManager(connectionManager)
                .setRetryHandler(retryHandler)
                .build();
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory() {
        // HttpComponentsClientHttpRequestFactory use Apache Http Client as backend implementation
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
                closeableHttpClient());
        clientHttpRequestFactory
                .setReadTimeout(applicationProperties.getHttpClient().getReadTimeout() * 1000);
        clientHttpRequestFactory.setConnectTimeout(1000);
        return clientHttpRequestFactory;
    }

    @Bean
    public RestTemplate globalRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(httpComponentsClientHttpRequestFactory());
        return restTemplate;
    }
}