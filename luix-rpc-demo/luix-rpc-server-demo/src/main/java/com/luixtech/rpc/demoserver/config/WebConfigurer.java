package com.luixtech.rpc.demoserver.config;

import com.luixtech.springbootframework.config.LuixProperties;
import com.luixtech.springbootframework.filter.CachingHttpHeadersFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

import static com.luixtech.springbootframework.config.LuixProperties.SPRING_PROFILE_PROD;
import static com.luixtech.springbootframework.config.LuixProperties.SPRING_PROFILE_TEST;
import static java.net.URLDecoder.decode;

/**
 * Web application configuration
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class WebConfigurer implements ServletContextInitializer, WebServerFactoryCustomizer<UndertowServletWebServerFactory> {
    private final Environment    env;
    private final LuixProperties luixProperties;

    @Override
    public void onStartup(ServletContext servletContext) {
        EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC);
        if (env.acceptsProfiles(Profiles.of(SPRING_PROFILE_PROD))) {
            initCachingHttpHeadersFilter(servletContext, types);
        }
        log.info("Configured web application");
    }

    /**
     * Customize the Servlet engine: Mime types, the document root, the cache.
     */
    @Override
    public void customize(UndertowServletWebServerFactory factory) {
        setMimeMappings(factory);
        if (Arrays.asList(env.getActiveProfiles()).contains(SPRING_PROFILE_TEST)) {
            // When running in an IDE or with ./mvnw spring-boot:run, set location of the static web assets.
            setLocationForStaticAssets(factory);
        }
    }

    private void setMimeMappings(WebServerFactory factory) {
        if (factory instanceof ConfigurableServletWebServerFactory servletWebServer) {
            MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
            // IE issue, see https://github.com/jhipster/generator-jhipster/pull/711
            mappings.add("html", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase());
            // CloudFoundry issue, see https://github.com/cloudfoundry/gorouter/issues/64
            mappings.add("json", MediaType.TEXT_HTML_VALUE + ";charset=" + StandardCharsets.UTF_8.name().toLowerCase());
            servletWebServer.setMimeMappings(mappings);
        }
    }

    private void setLocationForStaticAssets(WebServerFactory factory) {
        if (factory instanceof ConfigurableServletWebServerFactory servletWebServer) {
            File root;
            String prefixPath = resolvePathPrefix();
            root = new File(prefixPath + "src/main/webapp/");
            if (root.exists() && root.isDirectory()) {
                servletWebServer.setDocumentRoot(root);
            }
        }
    }

    /**
     * Resolve path prefix to static resources.
     */
    private String resolvePathPrefix() {
        String fullExecutablePath;
        fullExecutablePath = decode(Objects.requireNonNull(this.getClass().getResource("")).getPath(), StandardCharsets.UTF_8);
        String rootPath = Paths.get(".").toUri().normalize().getPath();
        String extractedPath = fullExecutablePath.replace(rootPath, "");
        int extractionEndIndex = extractedPath.indexOf("target/");
        if (extractionEndIndex <= 0) {
            return "";
        }
        return extractedPath.substring(0, extractionEndIndex);
    }

    /**
     * Initializes the caching HTTP Headers Filter.
     */
    private void initCachingHttpHeadersFilter(ServletContext servletContext, EnumSet<DispatcherType> types) {
        FilterRegistration.Dynamic cachingHttpHeadersFilter = servletContext.addFilter("cachingHttpHeadersFilter",
                new CachingHttpHeadersFilter(luixProperties));
        cachingHttpHeadersFilter.addMappingForUrlPatterns(types, true, "/i18n/*");
        cachingHttpHeadersFilter.addMappingForUrlPatterns(types, true, "/content/*");
        cachingHttpHeadersFilter.addMappingForUrlPatterns(types, true, "/app/*");
        cachingHttpHeadersFilter.setAsyncSupported(true);
        log.info("Registered caching HTTP headers filter");
    }
}