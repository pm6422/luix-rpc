package org.infinity.rpc.demoserver.controller;

import io.swagger.annotations.ApiOperation;
import org.infinity.rpc.demoserver.utils.NetworkUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class SystemController {

    @Resource
    private ApplicationContext applicationContext;

    @ApiOperation("get bean")
    @GetMapping("/api/system/bean")
    public ResponseEntity<Object> getBean(@RequestParam(value = "name") String name) {
        return ResponseEntity.ok(applicationContext.getBean(name));
    }

    @ApiOperation("get internet IP")
    @GetMapping("/api/system/internet-ip")
    public ResponseEntity<String> getInternetIp() {
        return ResponseEntity.ok(NetworkUtils.INTERNET_IP);
    }

    @ApiOperation("get intranet IP")
    @GetMapping("/api/system/intranet-ip")
    public ResponseEntity<String> getIntranetIp() {
        return ResponseEntity.ok(NetworkUtils.INTRANET_IP);
    }
}
