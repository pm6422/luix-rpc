package org.infinity.luix.portal.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class SystemController {

    private String luixDemoServerUrl;
    private String luixDemoClientUrl;
    private String luixWebCenterUrl;

    @GetMapping("/api/system/set-luix-demo-server-url")
    public void setLuixDemoServerUrl(@RequestParam(value = "url") String url) {
        this.luixDemoServerUrl = url;
    }

    @GetMapping("/api/system/get-luix-demo-server-url")
    public String getLuixDemoServerUrl() {
        return this.luixDemoServerUrl;
    }

    @GetMapping("/api/system/set-luix-demo-client-url")
    public void setLuixDemoClientUrl(@RequestParam(value = "url") String url) {
        this.luixDemoClientUrl = url;
    }

    @GetMapping("/api/system/get-luix-demo-client-url")
    public String getLuixDemoClientUrl() {
        return this.luixDemoClientUrl;
    }

    @GetMapping("/api/system/set-luix-web-center-url")
    public void setLuixWebCenterUrl(@RequestParam(value = "url") String url) {
        this.luixWebCenterUrl = url;
    }

    @GetMapping("/api/system/get-luix-web-center-url")
    public String getLuixWebCenterUrl() {
        return this.luixWebCenterUrl;
    }
}
