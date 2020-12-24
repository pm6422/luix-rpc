package org.infinity.rpc.appclient.controller;

import io.swagger.annotations.Api;
import org.infinity.rpc.appclient.config.ApplicationProperties;
import org.infinity.rpc.utilities.network.NetworkUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "系统")
public class SystemController {

    private final ApplicationProperties applicationProperties;

    public SystemController(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
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
