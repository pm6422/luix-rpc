package org.infinity.rpc.appserver.controller;

import io.swagger.annotations.Api;
import org.infinity.rpc.appserver.config.ApplicationProperties;
import org.infinity.rpc.appserver.utils.NetworkIpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "系统")
public class SystemController {

    @Autowired
    private ApplicationProperties applicationProperties;

    @GetMapping("/api/system/internet-ip")
    public ResponseEntity<String> getInternetIp() {
        return ResponseEntity.ok(NetworkIpUtils.INTERNET_IP);
    }

    @GetMapping("/api/system/intranet-ip")
    public ResponseEntity<String> getIntranetIp() {
        return ResponseEntity.ok(NetworkIpUtils.INTRANET_IP);
    }
}
