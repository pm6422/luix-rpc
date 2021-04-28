package org.infinity.rpc.webcenter.controller;

import io.changock.runner.core.ChangockBase;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.utils.NetworkUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Api(tags = "系统")
@Slf4j
public class SystemController {

    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private ChangockBase       changockBase;
    @Resource
    private MongoTemplate      mongoTemplate;

    @ApiOperation("get bean")
    @GetMapping("/api/system/bean")
    public ResponseEntity<Object> getBean(@RequestParam(value = "name") String name) {
        return ResponseEntity.ok(applicationContext.getBean(name));
    }

    @GetMapping("/api/system/internet-ip")
    @Secured(Authority.DEVELOPER)
    public ResponseEntity<String> getInternetIp() {
        return ResponseEntity.ok(NetworkUtils.INTERNET_IP);
    }

    @GetMapping("/api/system/intranet-ip")
    @Secured(Authority.DEVELOPER)
    public ResponseEntity<String> getIntranetIp() {
        return ResponseEntity.ok(NetworkUtils.INTRANET_IP);
    }

    @GetMapping("/open-api/system/reset-database")
    public String resetDatabase() {
        mongoTemplate.getDb().drop();
        changockBase.execute();
        return "Reset successfully.";
    }
}
