package org.infinity.luix.democlient.controller;

import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.infinity.luix.democlient.config.ApplicationProperties;
import org.infinity.luix.democlient.dto.SystemDTO;
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
    @Autowired(required = false)
    private BuildProperties       buildProperties;

    @ApiOperation("get system info")
    @GetMapping("/open-api/systems/info")
    public ResponseEntity<SystemDTO> getSystemInfo() {
        String id = buildProperties != null ? buildProperties.getArtifact() : appId;
        String version = buildProperties != null ? buildProperties.getVersion() : appVersion;
        SystemDTO systemDTO = new SystemDTO(id, version, companyName, getRibbonProfile(),
                applicationProperties.getSwagger().isEnabled(), env.getActiveProfiles());
        return ResponseEntity.ok(systemDTO);
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
}
