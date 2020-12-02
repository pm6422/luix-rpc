package org.infinity.rpc.appclient.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.service.AppService;
import org.infinity.rpc.core.config.spring.client.ConsumerWrapperBeanNameBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @ApiOperation("测试获取AppService consumer wrapper")
    @GetMapping("/open-api/test/app-service-consumer-wrapper")
    public Object testGetAppServiceConsumerWrapper() {
        return applicationContext.getBean(ConsumerWrapperBeanNameBuilder.CONSUMER_WRAPPER_BEAN_PREFIX.concat(":").concat(AppService.class.getName()));
    }
}
