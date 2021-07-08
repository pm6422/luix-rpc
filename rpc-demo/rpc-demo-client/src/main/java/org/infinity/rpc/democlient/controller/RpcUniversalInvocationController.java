package org.infinity.rpc.democlient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.ConsumerStubHolder;
import org.infinity.rpc.democlient.dto.MethodInvocation;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.infinity.rpc.core.constant.ProtocolConstants.SERIALIZER;
import static org.infinity.rpc.core.constant.ServiceConstants.*;
import static org.infinity.rpc.utilities.serializer.Serializer.SERIALIZER_NAME_HESSIAN2;

/**
 * REST controller for RPC calling.
 */
@RestController
@Slf4j
public class RpcUniversalInvocationController {

    @Resource
    private InfinityProperties infinityProperties;

    @ApiOperation("universal invocation")
    @PostMapping("/api/rpc/universal-invocation")
    public ResponseEntity<Object> universalInvoke(@ApiParam(value = "file", required = true) @RequestPart MultipartFile file) throws IOException {
        String input = StreamUtils.copyToString(file.getInputStream(), Charset.defaultCharset());
        MethodInvocation methodInvocation = new ObjectMapper().readValue(input, MethodInvocation.class);
        ConsumerStub<?> consumerStub = getConsumerStub(methodInvocation);
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler universalInvocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = universalInvocationHandler.invoke(methodInvocation.getMethodName(), methodInvocation.getMethodParamTypes(), methodInvocation.getArgs());
        return ResponseEntity.ok().body(result);
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
        ConsumerStub<?> consumerStub = ConsumerStub.create(data.getInterfaceName(), infinityProperties.getApplication(),
                infinityProperties.getRegistry(), infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, null, data.getAttributes().get(FORM), data.getAttributes().get(VERSION),
                requestTimeout, retryCount, defaultIfEmpty(data.getAttributes().get(SERIALIZER), SERIALIZER_NAME_HESSIAN2));
        ConsumerStubHolder.getInstance().add(beanName, consumerStub);
        return consumerStub;
    }
}
