package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.ConsumerStubHolder;
import org.infinity.rpc.webcenter.dto.UniversalMethodInvocation;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static org.infinity.rpc.core.constant.ServiceConstants.*;

/**
 * REST controller for RPC calling.
 */
@RestController
@Slf4j
public class RpcInvocationController {

    @Resource
    private InfinityProperties infinityProperties;

    @ApiOperation("universal invocation")
    @PostMapping("/api/rpc/universal-invocation")
    public ResponseEntity<Object> universalInvoke(
            @ApiParam(value = "arguments", required = true) @RequestBody UniversalMethodInvocation data) {
        ConsumerStub<?> consumerStub = getConsumerStub(data);
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler universalInvocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = universalInvocationHandler.invoke(data.getMethodName(), data.getMethodParamTypes(), data.getArgs());
        return ResponseEntity.ok().body(result);
    }

    private ConsumerStub<?> getConsumerStub(UniversalMethodInvocation data) {
        Map<String, Object> attributesMap = new HashMap<>(0);
        if (MapUtils.isNotEmpty(data.getAttributes())) {
            for (Map.Entry<String, String> entry : data.getAttributes().entrySet()) {
                attributesMap.put(entry.getKey(), entry.getValue());
            }
        }
        String beanName = ConsumerStub.buildConsumerStubBeanName(data.getInterfaceName(), attributesMap);
        if (ConsumerStubHolder.getInstance().get().containsKey(beanName)) {
            return ConsumerStubHolder.getInstance().get().get(beanName);
        }

        Integer requestTimeout = null;
        if (data.getAttributes().containsKey(REQUEST_TIMEOUT)) {
            requestTimeout = Integer.parseInt(data.getAttributes().get(REQUEST_TIMEOUT));
        }
        Integer retryCount = null;
        if (data.getAttributes().containsKey(RETRY_COUNT)) {
            retryCount = Integer.parseInt(data.getAttributes().get(RETRY_COUNT));
        }
        ConsumerStub<?> consumerStub = ConsumerStub.create(data.getInterfaceName(), infinityProperties.getApplication(),
                infinityProperties.getRegistry(), infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, null, data.getAttributes().get(FORM), data.getAttributes().get(VERSION),
                requestTimeout, retryCount);
        ConsumerStubHolder.getInstance().add(beanName, consumerStub);
        return consumerStub;
    }
}
