package com.luixtech.rpc.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "set luix demo server url")
    @PostMapping("/api/system/luix-demo-server-url")
    public void setLuixDemoServerUrl(@RequestParam(value = "url") String url) {
        this.luixDemoServerUrl = url;
    }

    @Operation(summary = "get luix demo server url")
    @GetMapping("/api/system/luix-demo-server-url")
    public String getLuixDemoServerUrl() {
        return this.luixDemoServerUrl;
    }

    @Operation(summary = "set luix demo client url")
    @PostMapping("/api/system/luix-demo-client-url")
    public void setLuixDemoClientUrl(@RequestParam(value = "url") String url) {
        this.luixDemoClientUrl = url;
    }

    @Operation(summary = "get luix demo client url")
    @GetMapping("/api/system/luix-demo-client-url")
    public String getLuixDemoClientUrl() {
        return this.luixDemoClientUrl;
    }

    @Operation(summary = "set luix RPC web center url")
    @PostMapping("/api/system/luix-web-center-url")
    public void setLuixWebCenterUrl(@RequestParam(value = "url") String url) {
        this.luixWebCenterUrl = url;
    }

    @Operation(summary = "get luix RPC web center url")
    @GetMapping("/api/system/luix-web-center-url")
    public String getLuixWebCenterUrl() {
        return this.luixWebCenterUrl;
    }

    @Operation(summary = "set luix registry url")
    @PostMapping("/api/system/luix-registry-url")
    public void setLuixRegistryUrl(@RequestParam(value = "url") String url) {
        this.luixRegistryUrl = url;
    }

    @Operation(summary = "get luix registry url")
    @GetMapping("/api/system/luix-registry-url")
    public String getLuixRegistryUrl() {
        return this.luixRegistryUrl;
    }

    @Operation(summary = "get monitor url")
    @GetMapping("/api/system/monitor-url")
    public String getMonitorUrl() {
        return this.monitorUrl;
    }

    @Operation(summary = "reset RPC web center database")
    @GetMapping("/api/system/reset-web-center-database")
    public String resetWebCenterDatabase() {
        return this.luixWebCenterUrl + "/open-api/systems/reset-database";
    }

    @Operation(summary = "reset demo server database")
    @GetMapping("/api/system/reset-demo-server-database")
    public String resetDemoServerDatabase() {
        return this.luixDemoServerUrl + "/open-api/systems/reset-database";
    }
}
