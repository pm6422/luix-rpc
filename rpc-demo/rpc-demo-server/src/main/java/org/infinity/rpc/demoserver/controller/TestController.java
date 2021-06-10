package org.infinity.rpc.demoserver.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democommon.service.AppService;
import org.infinity.rpc.democommon.service.AuthorityService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static org.infinity.rpc.core.constant.ApplicationConstants.APP;

@RestController
@Slf4j
public class TestController {
    @Resource
    private InfinityProperties infinityProperties;

    @ApiOperation("register provider")
    @GetMapping("/api/test/register-provider")
    public void registerProvider() {
        Url providerUrl = Url.of(
                infinityProperties.getAvailableProtocol().getName(),
                "192.168.0.1",
                infinityProperties.getAvailableProtocol().getPort(),
                AppService.class.getName());

        // Assign values to parameters
        providerUrl.addOption(APP, infinityProperties.getApplication().getName());

        infinityProperties.getRegistryList().forEach(registryConfig -> {
            registryConfig.getRegistryImpl().register(providerUrl);
        });
    }

    @Resource
    private AuthorityService authorityService;

    @ApiOperation("test aspect")
    @GetMapping("/api/test/aspect")
    public void testAspect() {
        authorityService.findAllAuthorityNames(true);
        authorityService.findAllAuthorityNames();
    }
}
