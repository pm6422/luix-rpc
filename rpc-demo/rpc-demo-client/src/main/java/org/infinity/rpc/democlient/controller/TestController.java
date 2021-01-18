package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.service.AppService;
import org.infinity.rpc.core.config.spring.client.stub.ConsumerStubBeanNameBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "测试")
@Slf4j
public class TestController {

    private final ApplicationContext applicationContext;

    public TestController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @ApiOperation("测试获取AppService consumer stub")
    @GetMapping("/open-api/test/app-service-consumer-stub")
    public Object testGetAppServiceConsumerStub() {
        return applicationContext.getBean(ConsumerStubBeanNameBuilder.CONSUMER_STUB_BEAN_PREFIX.concat(":").concat(AppService.class.getName()));
    }
}
