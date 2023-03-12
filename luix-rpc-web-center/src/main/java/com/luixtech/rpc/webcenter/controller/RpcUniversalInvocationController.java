package com.luixtech.rpc.webcenter.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luixtech.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import com.luixtech.rpc.core.client.proxy.Proxy;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.spring.boot.starter.config.LuixRpcProperties;
import com.luixtech.rpc.webcenter.dto.MethodInvocation;
import com.luixtech.rpc.webcenter.service.RpcRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.Charset;

import static com.luixtech.rpc.webcenter.config.api.SpringDocConfiguration.AUTH;

/**
 * REST controller for RPC universal calling.
 */
@RestController
@SecurityRequirement(name = AUTH)
@AllArgsConstructor
@Slf4j
public class RpcUniversalInvocationController {
    private final LuixRpcProperties  luixRpcProperties;
    private final RpcRegistryService rpcRegistryService;

    @Operation(summary = "direct address invocation")
    @PostMapping("/api/rpc-invocations/invoke")
    public String invoke(@Parameter(description = "methodInvocation", required = true)
                         @Valid @RequestBody MethodInvocation methodInvocation) throws JsonProcessingException {
        ConsumerStub<?> consumerStub = rpcRegistryService.getConsumerStub(methodInvocation.getRegistryIdentity(),
                methodInvocation.getProviderUrl(), methodInvocation.getInterfaceName(), methodInvocation.getAttributes());
        Proxy proxyFactory = Proxy.getInstance(luixRpcProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = invocationHandler.invoke(methodInvocation.getMethodName(), methodInvocation.getMethodParamTypes(), methodInvocation.getArgs());
        return new ObjectMapper().writeValueAsString(result);
    }

    @Operation(summary = "discover address invocation by file")
    @PostMapping("/api/rpc-invocations/invoke-by-file")
    public String invokeByFile(@Parameter(description = "file", required = true) @RequestPart MultipartFile file) throws IOException {
        String input = StreamUtils.copyToString(file.getInputStream(), Charset.defaultCharset());
        MethodInvocation methodInvocation = new ObjectMapper().readValue(input, MethodInvocation.class);
        ConsumerStub<?> consumerStub = rpcRegistryService.getConsumerStub(methodInvocation.getRegistryIdentity(),
                methodInvocation.getProviderUrl(), methodInvocation.getInterfaceName(), methodInvocation.getAttributes());
        Proxy proxyFactory = Proxy.getInstance(luixRpcProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = invocationHandler.invoke(methodInvocation.getMethodName(), methodInvocation.getMethodParamTypes(), methodInvocation.getArgs());
        return new ObjectMapper().writeValueAsString(result);
    }
}
