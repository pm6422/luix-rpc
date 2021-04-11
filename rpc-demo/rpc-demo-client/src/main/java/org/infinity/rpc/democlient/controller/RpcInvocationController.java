package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.ConsumerStubHolder;
import org.infinity.rpc.core.client.stub.MethodInvocationData;
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
     * "attributes": {
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
    public ResponseEntity<Object> universalInvoke(@ApiParam(value = "调用参数", required = true) @Valid @RequestBody MethodInvocationData dto) {
        ConsumerStub<?> consumerStub = getConsumerStub(dto);
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler universalInvocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = universalInvocationHandler.invoke(dto.getMethodName(), dto.getMethodParamTypes(), dto.getArgs());
        return ResponseEntity.ok().body(result);
    }

    private ConsumerStub<?> getConsumerStub(MethodInvocationData dto) {
        Map<String, Object> attributesMap = new HashMap<>(0);
        if (MapUtils.isNotEmpty(dto.getAttributes())) {
            for (Map.Entry<String, String> entry : dto.getAttributes().entrySet()) {
                attributesMap.put(entry.getKey(), entry.getValue());
            }
        }
        String beanName = ConsumerStub.buildConsumerStubBeanName(dto.getInterfaceName(), attributesMap);
        if (ConsumerStubHolder.getInstance().getStubs().containsKey(beanName)) {
            return ConsumerStubHolder.getInstance().getStubs().get(beanName);
        }

        Integer requestTimeout = null;
        if (dto.getAttributes().containsKey(REQUEST_TIMEOUT)) {
            requestTimeout = Integer.parseInt(dto.getAttributes().get(REQUEST_TIMEOUT));
        }
        Integer maxRetries = null;
        if (dto.getAttributes().containsKey(MAX_RETRIES)) {
            maxRetries = Integer.parseInt(dto.getAttributes().get(MAX_RETRIES));
        }
        ConsumerStub<?> consumerStub = ConsumerStub.create(dto.getInterfaceName(), infinityProperties.getApplication(),
                infinityProperties.getRegistry(), infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, dto.getAttributes().get(FORM), dto.getAttributes().get(VERSION), requestTimeout, maxRetries);
        ConsumerStubHolder.getInstance().addStub(beanName, consumerStub);
        return consumerStub;
    }
}
