package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.webcenter.dto.MethodInvocation;
import org.infinity.rpc.webcenter.service.RpcRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * REST controller for RPC universal calling.
 */
@RestController
@Slf4j
public class RpcUniversalInvocationController {

    @Resource
    private InfinityProperties infinityProperties;
    @Resource
    private RpcRegistryService rpcRegistryService;

    @ApiOperation("universal invocation")
    @PostMapping("/api/rpc-invocation/invoke")
    public ResponseEntity<Object> invoke(@ApiParam(value = "methodInvocation", required = true)
                                         @Valid @RequestBody MethodInvocation methodInvocation) {
        ConsumerStub<?> consumerStub = rpcRegistryService.getConsumerStub(methodInvocation.getRegistryIdentity(),
                Url.valueOf(methodInvocation.getProviderUrl()), methodInvocation.getInterfaceName(), methodInvocation.getAttributes());
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = invocationHandler.invoke(methodInvocation.getMethodName(), methodInvocation.getMethodParamTypes(), methodInvocation.getArgs());
        return ResponseEntity.ok().body(result);
    }
}
