package com.luixtech.rpc.democlient.controller;

import com.luixtech.rpc.democlient.config.ApplicationProperties;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class SystemController {

    @Resource
    private Environment           env;
    @Resource
    private ApplicationProperties applicationProperties;
    @Resource
    private ApplicationContext    applicationContext;
    @Value("${app.id}")
    private String                appId;
    @Value("${app.version}")
    private String                appVersion;
    @Value("${app.companyName}")
    private String                companyName;
    @Value("${springdoc.api-docs.enabled}")
    private boolean               enabledApiDocs;
    @Autowired(required = false)
    private BuildProperties       buildProperties;

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

        return String.format(js, id, version, companyName, getRibbonProfile(), enabledApiDocs);
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

    @Operation(summary = "get bean")
    @GetMapping("/api/systems/bean")
    public ResponseEntity<Object> getBean(@RequestParam(value = "name") String name) {
        return ResponseEntity.ok(applicationContext.getBean(name));
    }
}
