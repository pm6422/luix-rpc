package org.infinity.rpc.appserver.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.service.AppService;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@Api(tags = "测试")
@Slf4j
public class TestController {
    @Autowired
    private List<Url> registryUrls;
    @Autowired
    private InfinityProperties infinityProperties;

    /**
     * 测试结果显示为线程安全
     *
     * @param key
     * @throws InterruptedException
     */
    @ApiOperation("测试Request线程安全")
    @GetMapping("/open-api/test/threadsafe")
    public void testThreadSafe(@RequestParam(value = "key", required = true) String key) throws InterruptedException {
        Set<String> keys = new HashSet<>();
        if (keys.contains(key)) {
            log.error("Key {} already existed, request is not threadsafe!", key);
        } else {
            log.debug(key);
            keys.add(key);
        }

        TimeUnit.MILLISECONDS.sleep(1000);
    }

    @ApiOperation("测试注册provider")
    @GetMapping("/open-api/test/register-provider")
    public void registerProvider() {
        Registry registry = RegistryFactory.getInstance(infinityProperties.getRegistry().getName().value()).getRegistry(registryUrls.get(0));
        Url providerUrl = Url.of(
                infinityProperties.getProtocol().getName().value(),
                "192.168.0.1",
                infinityProperties.getProtocol().getPort(),
                AppService.class.getName());

        // Assign values to parameters
        providerUrl.addParameter(Url.PARAM_CHECK_HEALTH, Url.PARAM_CHECK_HEALTH_DEFAULT_VALUE);
        providerUrl.addParameter(Url.PARAM_APP, infinityProperties.getApplication().getName());

        registry.register(providerUrl);
    }
}
