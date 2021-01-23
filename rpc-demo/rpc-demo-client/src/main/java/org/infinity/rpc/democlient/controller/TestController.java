package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.democommon.service.AppService;
import org.infinity.rpc.spring.boot.bean.name.ConsumerStubBeanNameBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.infinity.rpc.core.constant.ServiceConstants.GROUP_DEFAULT_VALUE;
import static org.infinity.rpc.core.constant.ServiceConstants.VERSION_DEFAULT_VALUE;

@RestController
@Api(tags = "测试")
@Slf4j
public class TestController {

    private final ApplicationContext applicationContext;
    private final Environment        env;

    public TestController(ApplicationContext applicationContext, Environment env) {
        this.applicationContext = applicationContext;
        this.env = env;
    }

    @ApiOperation("测试获取AppService consumer stub")
    @GetMapping("/open-api/test/app-service-consumer-stub")
    public Object testGetAppServiceConsumerStub() {
        String name = ConsumerStubBeanNameBuilder
                .builder(AppService.class.getName(), env)
                .group(GROUP_DEFAULT_VALUE)
                .version(VERSION_DEFAULT_VALUE)
                .build();
        return applicationContext.getBean(name);
    }
}
