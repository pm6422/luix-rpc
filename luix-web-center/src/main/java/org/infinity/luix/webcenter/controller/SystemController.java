package org.infinity.luix.webcenter.controller;

import com.github.cloudyrock.mongock.runner.core.executor.MongockRunnerBase;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.infinity.luix.utilities.network.AddressUtils;
import org.infinity.luix.webcenter.config.ApplicationProperties;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.dto.SystemDTO;
import org.infinity.luix.webcenter.utils.NetworkUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @Autowired(required = false)
    private MongockRunnerBase<?>  mongockRunnerBase;
    @Resource
    private MongoTemplate         mongoTemplate;
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
        mongockRunnerBase.execute();
        return "Reset database successfully.";
    }
}
