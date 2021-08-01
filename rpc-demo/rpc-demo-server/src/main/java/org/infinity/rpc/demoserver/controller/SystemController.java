package org.infinity.rpc.demoserver.controller;

import io.swagger.annotations.ApiOperation;
import org.infinity.rpc.demoserver.config.ApplicationProperties;
import org.infinity.rpc.demoserver.dto.ProfileInfoDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class SystemController {

    @Resource
    private Environment           env;
    @Resource
    private ApplicationProperties applicationProperties;
    @Resource
    private ApplicationContext    applicationContext;

    @ApiOperation("get profile")
    @GetMapping("/open-api/system/profile-info")
    public ResponseEntity<ProfileInfoDTO> getProfileInfo() {
        ProfileInfoDTO profileInfoDTO = new ProfileInfoDTO(env.getActiveProfiles(), applicationProperties.getSwagger().isEnabled(), getRibbonEnv());
        return ResponseEntity.ok(profileInfoDTO);
    }

    private String getRibbonEnv() {
        String[] activeProfiles = env.getActiveProfiles();
        String[] displayOnActiveProfiles = applicationProperties.getRibbon().getDisplayOnActiveProfiles();
        if (displayOnActiveProfiles == null) {
            return null;
        }

        List<String> ribbonProfiles = new ArrayList<>(Arrays.asList(displayOnActiveProfiles));
        List<String> springBootProfiles = Arrays.asList(activeProfiles);
        ribbonProfiles.retainAll(springBootProfiles);

        if (ribbonProfiles.size() > 0) {
            return ribbonProfiles.get(0);
        }
        return null;
    }

    @ApiOperation("get bean")
    @GetMapping("/api/system/bean")
    public ResponseEntity<Object> getBean(@RequestParam(value = "name") String name) {
        return ResponseEntity.ok(applicationContext.getBean(name));
    }
}
