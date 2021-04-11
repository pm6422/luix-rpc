package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.ConsumerStubHolder;
import org.infinity.rpc.core.utils.name.ConsumerStubBeanNameBuilder;
import org.infinity.rpc.democlient.dto.UniversalInvokeDTO;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.infinity.rpc.core.constant.ServiceConstants.*;

/**
 * REST controller for RPC calling.
 */
@RestController
@Api(tags = "RPC调用")
@Slf4j
public class RpcInvocationController {

    @Resource
    private InfinityProperties infinityProperties;

    /**
     * {
     * "interfaceName": "org.infinity.rpc.democommon.service.AuthorityService",
     * "methodName": "findAll",
     * "methodParamTypes": [],
     * "args": [],
     * "options": {
     * "group": "default",
     * "version": "1.0.0"
     * }
     * }
     * <p>
     * {
     * "interfaceName": "org.infinity.rpc.democommon.service.AuthorityService",
     * "methodName": "save",
     * "methodParamTypes": ["org.infinity.rpc.democommon.domain.Authority"],
     * "args": [{
     * "name": "ROLE_TEST",
     * "enabled": true
     * }],
     * "options": {
     * "group": "default",
     * "version": "1.0.0"
     * }
     * }
     *
     * @param dto dto
     * @return result
     */
    @ApiOperation("通用调用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功调用")})
    @PostMapping("/api/rpc/universal-invocation")
    public ResponseEntity<Object> universalInvoke(@ApiParam(value = "调用参数", required = true) @Valid @RequestBody UniversalInvokeDTO dto) {
        ConsumerStub<?> consumerStub = getConsumerStub(dto);
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler universalInvocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = universalInvocationHandler.invoke(dto.getMethodName(), dto.getMethodParamTypes(), dto.getArgs());
        return ResponseEntity.ok().body(result);
    }

    private ConsumerStub<?> getConsumerStub(UniversalInvokeDTO dto) {
        Map<String, Object> optionMap = new HashMap<>(dto.getOptions());
        for (Map.Entry<String, String> entry : dto.getOptions().entrySet()) {
            optionMap.put(entry.getKey(), entry.getValue());
        }
        String beanName = ConsumerStubBeanNameBuilder
                .builder(dto.getInterfaceName())
                .attributes(optionMap)
                .build();

        if (ConsumerStubHolder.getInstance().getStubs().containsKey(beanName)) {
            return ConsumerStubHolder.getInstance().getStubs().get(beanName);
        }
        ConsumerStub<?> consumerStub = new ConsumerStub<>();
        consumerStub.setInterfaceName(dto.getInterfaceName());
        consumerStub.setProtocol(infinityProperties.getAvailableProtocol().getName());
        consumerStub.setCluster(infinityProperties.getConsumer().getCluster());
        consumerStub.setFaultTolerance(infinityProperties.getConsumer().getFaultTolerance());
        consumerStub.setLoadBalancer(infinityProperties.getConsumer().getLoadBalancer());
        consumerStub.setProxy(infinityProperties.getConsumer().getProxyFactory());
        consumerStub.setHealthChecker(infinityProperties.getConsumer().getHealthChecker());
        if (dto.getOptions().containsKey(FORM)) {
            consumerStub.setForm(dto.getOptions().get(FORM));
        }
        if (dto.getOptions().containsKey(VERSION)) {
            consumerStub.setVersion(dto.getOptions().get(VERSION));
        }
        if (dto.getOptions().containsKey(REQUEST_TIMEOUT)) {
            consumerStub.setRequestTimeout(Integer.parseInt(dto.getOptions().get(REQUEST_TIMEOUT)));
        }
        if (dto.getOptions().containsKey(MAX_RETRIES)) {
            consumerStub.setMaxRetries(Integer.parseInt(dto.getOptions().get(MAX_RETRIES)));
        }

        // Must NOT call init()

        consumerStub.subscribeProviders(infinityProperties.getApplication(),
                infinityProperties.getAvailableProtocol(),
                infinityProperties.getRegistry());

        ConsumerStubHolder.getInstance().addStub(beanName, consumerStub);
        return consumerStub;
    }
}
