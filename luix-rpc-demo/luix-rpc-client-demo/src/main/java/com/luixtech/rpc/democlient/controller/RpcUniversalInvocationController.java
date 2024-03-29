package com.luixtech.rpc.democlient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luixtech.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import com.luixtech.rpc.core.client.proxy.Proxy;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.core.client.stub.ConsumerStubFactory;
import com.luixtech.rpc.core.client.stub.ConsumerStubHolder;
import com.luixtech.rpc.democlient.dto.MethodInvocation;
import com.luixtech.rpc.spring.boot.starter.config.LuixRpcProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static com.luixtech.rpc.core.constant.ProtocolConstants.SERIALIZER;
import static com.luixtech.rpc.core.constant.ServiceConstants.*;
import static com.luixtech.rpc.serializer.Serializer.SERIALIZER_NAME_HESSIAN2;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

/**
 * REST controller for RPC calling.
 */
@RestController
@AllArgsConstructor
@Slf4j
public class RpcUniversalInvocationController {
    private LuixRpcProperties luixRpcProperties;

    @Operation(summary = "discover address invocation")
    @PostMapping("/api/rpc/universal-invocation")
    public String universalInvoke(@Parameter(description = "file", required = true) @RequestPart MultipartFile file) throws IOException {
        String input = StreamUtils.copyToString(file.getInputStream(), Charset.defaultCharset());
        MethodInvocation methodInvocation = new ObjectMapper().readValue(input, MethodInvocation.class);
        ConsumerStub<?> consumerStub = getConsumerStub(methodInvocation);
        Proxy proxyFactory = Proxy.getInstance(luixRpcProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler universalInvocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = universalInvocationHandler.invoke(methodInvocation.getMethodName(), methodInvocation.getMethodParamTypes(), methodInvocation.getArgs());
        return new ObjectMapper().writeValueAsString(result);
    }

    private ConsumerStub<?> getConsumerStub(MethodInvocation data) {
        Map<String, Object> attributesMap = new HashMap<>(0);
        if (MapUtils.isNotEmpty(data.getAttributes())) {
            for (Map.Entry<String, String> entry : data.getAttributes().entrySet()) {
                attributesMap.put(entry.getKey(), entry.getValue());
            }
        }
        String beanName = ConsumerStub.buildConsumerStubBeanName(data.getInterfaceName(), attributesMap);
//        if (ConsumerStubHolder.getInstance().get().containsKey(beanName)) {
//            return ConsumerStubHolder.getInstance().get().get(beanName);
//        }

        Integer requestTimeout = null;
        if (data.getAttributes().containsKey(REQUEST_TIMEOUT)) {
            requestTimeout = Integer.parseInt(data.getAttributes().get(REQUEST_TIMEOUT));
        }
        Integer retryCount = null;
        if (data.getAttributes().containsKey(RETRY_COUNT)) {
            retryCount = Integer.parseInt(data.getAttributes().get(RETRY_COUNT));
        }
        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(luixRpcProperties.getApplication(), luixRpcProperties.getRegistry(),
                luixRpcProperties.getAvailableProtocol(), data.getInterfaceName(),
                defaultIfEmpty(data.getAttributes().get(SERIALIZER), SERIALIZER_NAME_HESSIAN2),
                data.getAttributes().get(FORM), data.getAttributes().get(VERSION), requestTimeout, retryCount);
        ConsumerStubHolder.getInstance().add(beanName, consumerStub);
        return consumerStub;
    }
}
