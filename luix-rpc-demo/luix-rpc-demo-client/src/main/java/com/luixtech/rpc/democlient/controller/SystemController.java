package com.luixtech.rpc.democlient.controller;

import com.luixtech.springbootframework.config.LuixProperties;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Optional;

@RestController
public class SystemController {
    @Resource
    private Environment               env;
    @Resource
    private LuixProperties            luixProperties;
    @Resource
    private ApplicationContext        applicationContext;
    @Value("${app.id}")
    private String                    appId;
    @Value("${app.version}")
    private String                    appVersion;
    @Value("${app.companyName}")
    private String                    companyName;
    @Value("${springdoc.api-docs.enabled}")
    private boolean                   enabledApiDocs;
    @Resource
    private Optional<BuildProperties> buildProperties;

    @GetMapping(value = "app/constants.js", produces = "application/javascript")
    public String getConstantsJs() {
        String id = buildProperties.isPresent() ? buildProperties.get().getArtifact() : appId;
        String version = buildProperties.isPresent() ? buildProperties.get().getVersion() : appVersion;
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
        List<String> displayOnActiveProfiles = luixProperties.getRibbon().getDisplayOnActiveProfiles();
        if (CollectionUtils.isEmpty(displayOnActiveProfiles)) {
            return null;
        }

        displayOnActiveProfiles.retainAll(Arrays.asList(env.getActiveProfiles()));

        return CollectionUtils.isNotEmpty(displayOnActiveProfiles) ? displayOnActiveProfiles.get(0) : StringUtils.EMPTY;
    }

    @Operation(summary = "get bean")
    @GetMapping("/api/systems/bean")
    public ResponseEntity<Object> getBean(@RequestParam(value = "name") String name) {
        return ResponseEntity.ok(applicationContext.getBean(name));
    }
}
