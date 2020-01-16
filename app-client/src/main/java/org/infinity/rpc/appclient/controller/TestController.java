package org.infinity.rpc.appclient.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@Api(tags = "测试")
public class TestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestController.class);

    /**
     * 测试结果显示为线程安全
     *
     * @param key
     * @throws InterruptedException
     */
    @ApiOperation("测试Request线程安全")
    @GetMapping("/open-api/test/threadsafe")
    @Timed
    public void testThreadSafe(@RequestParam(value = "key", required = true) String key) throws InterruptedException {
        Set<String> keys = new HashSet<>();
        if (keys.contains(key)) {
            LOGGER.error("Key {} already existed, request is not threadsafe!", key);
        } else {
            LOGGER.debug(key);
            keys.add(key);
        }

        TimeUnit.MILLISECONDS.sleep(1000);
    }
}
