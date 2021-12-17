package org.infinity.luix.demoserver.controller;

import io.changock.runner.core.ChangockBase;
import io.swagger.annotations.ApiOperation;
import org.infinity.luix.demoserver.config.ApplicationProperties;
import org.infinity.luix.demoserver.dto.ProfileInfoDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
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
    @Resource
    private ChangockBase          changockBase;
    @Resource
    private MongoTemplate         mongoTemplate;

    @ApiOperation("get profile")
    @GetMapping("/open-api/systems/profile-info")
    public ResponseEntity<ProfileInfoDTO> getProfileInfo() {
        ProfileInfoDTO profileInfoDTO = new ProfileInfoDTO(env.getActiveProfiles(),
                applicationProperties.getSwagger().isEnabled(), getRibbonEnv());
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
    @GetMapping("/api/systems/bean")
    public ResponseEntity<Object> getBean(@RequestParam(value = "name") String name) {
        return ResponseEntity.ok(applicationContext.getBean(name));
    }

    @ApiOperation("reset database")
    @GetMapping("/open-api/systems/reset-database")
    public String resetDatabase() {
        reset();
        return "Reset database successfully.";
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void reset() {
        mongoTemplate.getDb().drop();
        changockBase.execute();
    }
}
