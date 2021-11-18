package org.infinity.luix.webcenter.controller;

import io.changock.runner.core.ChangockBase;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.utilities.network.AddressUtils;
import org.infinity.luix.webcenter.config.ApplicationProperties;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.dto.ProfileInfoDTO;
import org.infinity.luix.webcenter.utils.NetworkUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
public class SystemController {

    @Value("${arthas.httpPort}")
    private int                   arthasPort;
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
        changockBase.execute();
        return "Reset database successfully.";
    }
}
