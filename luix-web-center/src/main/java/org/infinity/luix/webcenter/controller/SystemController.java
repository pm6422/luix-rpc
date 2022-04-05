package org.infinity.luix.webcenter.controller;

import io.mongock.api.config.MongockConfiguration;
import io.mongock.driver.api.driver.ConnectionDriver;
import io.mongock.driver.mongodb.springdata.v3.config.MongoDBConfiguration;
import io.mongock.driver.mongodb.springdata.v3.config.SpringDataMongoV3Context;
import io.mongock.runner.springboot.MongockSpringboot;
import io.mongock.runner.springboot.RunnerSpringbootBuilder;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.infinity.luix.utilities.network.AddressUtils;
import org.infinity.luix.webcenter.config.ApplicationProperties;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.utils.NetworkUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Slf4j
public class SystemController {

    @Value("${arthas.httpPort}")
    private int                                  arthasPort;
    @Resource
    private Environment                          env;
    @Resource
    private ApplicationProperties                applicationProperties;
    @Resource
    private ApplicationContext                   applicationContext;
    @Resource
    private MongoTemplate                        mongoTemplate;
    @Value("${app.id}")
    private String                               appId;
    @Value("${app.version}")
    private String                               appVersion;
    @Value("${app.companyName}")
    private String                               companyName;
    @Autowired(required = false)
    private BuildProperties                      buildProperties;
    @Resource
    private MongockConfiguration                 springConfiguration;
    @Resource
    private ApplicationEventPublisher            applicationEventPublisher;
    @Resource
    private MongockConfiguration                 config;
    @Resource
    private MongoDBConfiguration                 mongoDbConfig;
    @Resource
    private Optional<PlatformTransactionManager> txManagerOpt;

    @GetMapping(value = "app/constants.js", produces = "application/javascript")
    String getConstantsJs() {
        String id = buildProperties != null ? buildProperties.getArtifact() : appId;
        String version = buildProperties != null ? buildProperties.getVersion() : appVersion;
        String js = "'use strict';\n" +
                "(function () {\n" +
                "    'use strict';\n" +
                "    angular\n" +
                "        .module('smartcloudserviceApp')\n" +
                "        .constant('APP_NAME', '%s')\n" +
                "        .constant('VERSION', '%s')\n" +
                "        .constant('COMPANY', '%s')\n" +
                "        .constant('RIBBON_PROFILE', '%s')\n" +
                "        .constant('ENABLE_SWAGGER', '%s')\n" +
                "        .constant('PAGINATION_CONSTANTS', {\n" +
                "            'itemsPerPage': 10\n" +
                "        })\n" +
                "        .constant('DEBUG_INFO_ENABLED', true);\n" +
                "})();";

        return String.format(js, id, version, companyName, getRibbonProfile(),
                applicationProperties.getSwagger().isEnabled());
    }

    private String getRibbonProfile() {
        String[] displayOnActiveProfiles = applicationProperties.getRibbon().getDisplayOnActiveProfiles();
        if (ArrayUtils.isEmpty(displayOnActiveProfiles)) {
            return null;
        }

        List<String> ribbonProfiles = Stream.of(displayOnActiveProfiles).collect(Collectors.toList());
        ribbonProfiles.retainAll(Arrays.asList(env.getActiveProfiles()));

        return CollectionUtils.isNotEmpty(ribbonProfiles) ? ribbonProfiles.get(0) : null;
    }

    @ApiOperation("get bean")
    @GetMapping("/api/systems/bean")
    public ResponseEntity<Object> getBean(@RequestParam(value = "name") String name) {
        return ResponseEntity.ok(applicationContext.getBean(name));
    }

    @ApiOperation("get intranet ip")
    @GetMapping("/api/systems/intranet-ip")
    @Secured(Authority.DEVELOPER)
    public ResponseEntity<String> getIntranetIp() {
        return ResponseEntity.ok(AddressUtils.getIntranetIp());
    }

    @ApiOperation("redirect to arthas web console")
    @GetMapping("/api/system/arthas-console")
    @Secured(Authority.DEVELOPER)
    public void redirectToArthasConsole(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String arthasUrl = NetworkUtils.getRequestUrl(request, arthasPort);
        log.info("Redirect to arthas console: {}", arthasUrl);
        response.sendRedirect(arthasUrl);
    }

    @ApiOperation("reset database")
    @GetMapping("/open-api/systems/reset-database")
    public String resetDatabase() {
        mongoTemplate.getDb().drop();
        ConnectionDriver connectionDriver = new SpringDataMongoV3Context()
                .connectionDriver(mongoTemplate, config, mongoDbConfig, txManagerOpt);
        RunnerSpringbootBuilder runnerSpringbootBuilder = MongockSpringboot.builder()
                .setDriver(connectionDriver)
                .setConfig(springConfiguration)
                .setSpringContext(applicationContext)
                .setEventPublisher(applicationEventPublisher);
        runnerSpringbootBuilder.buildRunner().execute();
        return "Reset database successfully.";
    }
}
