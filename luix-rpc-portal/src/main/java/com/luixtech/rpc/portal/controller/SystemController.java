package com.luixtech.rpc.portal.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class SystemController {

    private String luixDemoServerUrl = "http://pm6422.club:6010";
    private String luixDemoClientUrl = "http://pm6422.club:6020";
    private String luixWebCenterUrl  = "http://pm6422.club:6030";
    private String luixRegistryUrl   = "http://pm6422.club:8500";
    private String monitorUrl        = "http://pm6422.club:3000";

    @ApiOperation("set luix demo server url")
    @PostMapping("/api/system/luix-demo-server-url")
    public void setLuixDemoServerUrl(@RequestParam(value = "url") String url) {
        this.luixDemoServerUrl = url;
    }

    @ApiOperation("get luix demo server url")
    @GetMapping("/api/system/luix-demo-server-url")
    public String getLuixDemoServerUrl() {
        return this.luixDemoServerUrl;
    }

    @ApiOperation("set luix demo client url")
    @PostMapping("/api/system/luix-demo-client-url")
    public void setLuixDemoClientUrl(@RequestParam(value = "url") String url) {
        this.luixDemoClientUrl = url;
    }

    @ApiOperation("get luix demo client url")
    @GetMapping("/api/system/luix-demo-client-url")
    public String getLuixDemoClientUrl() {
        return this.luixDemoClientUrl;
    }

    @ApiOperation("set luix RPC web center url")
    @PostMapping("/api/system/luix-web-center-url")
    public void setLuixWebCenterUrl(@RequestParam(value = "url") String url) {
        this.luixWebCenterUrl = url;
    }

    @ApiOperation("get luix RPC web center url")
    @GetMapping("/api/system/luix-web-center-url")
    public String getLuixWebCenterUrl() {
        return this.luixWebCenterUrl;
    }

    @ApiOperation("set luix registry url")
    @PostMapping("/api/system/luix-registry-url")
    public void setLuixRegistryUrl(@RequestParam(value = "url") String url) {
        this.luixRegistryUrl = url;
    }

    @ApiOperation("get luix registry url")
    @GetMapping("/api/system/luix-registry-url")
    public String getLuixRegistryUrl() {
        return this.luixRegistryUrl;
    }

    @ApiOperation("get monitor url")
    @GetMapping("/api/system/monitor-url")
    public String getMonitorUrl() {
        return this.monitorUrl;
    }

    @ApiOperation("reset RPC web center database")
    @GetMapping("/api/system/reset-web-center-database")
    public String resetWebCenterDatabase() {
        return this.luixWebCenterUrl + "/open-api/systems/reset-database";
    }

    @ApiOperation("reset demo server database")
    @GetMapping("/api/system/reset-demo-server-database")
    public String resetDemoServerDatabase() {
        return this.luixDemoServerUrl + "/open-api/systems/reset-database";
    }
}
