package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.infinity.rpc.utilities.network.NetworkUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "系统")
public class SystemController {

    private final ApplicationContext applicationContext;

    public SystemController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @GetMapping("/api/system/bean")
    public ResponseEntity<Object> getBean(@ApiParam(value = "bean名称", required = true) @RequestParam(value = "name") String name) {
        return ResponseEntity.ok(applicationContext.getBean(name));
    }

    @GetMapping("/api/system/internet-ip")
    public ResponseEntity<String> getInternetIp() {
        return ResponseEntity.ok(NetworkUtils.INTERNET_IP);
    }

    @GetMapping("/api/system/intranet-ip")
    public ResponseEntity<String> getIntranetIp() {
        return ResponseEntity.ok(NetworkUtils.INTRANET_IP);
    }
}
