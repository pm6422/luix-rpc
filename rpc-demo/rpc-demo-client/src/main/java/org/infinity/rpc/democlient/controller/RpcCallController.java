package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.GenericCallHandler;
import org.infinity.rpc.core.client.proxy.ProxyFactory;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.democlient.dto.GenericCallDTO;
import org.infinity.rpc.spring.boot.bean.name.ProviderStubBeanNameBuilder;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.infinity.rpc.core.constant.ServiceConstants.*;

/**
 * REST controller for RPC calling.
 */
@RestController
@Api(tags = "RPC调用")
@Slf4j
public class RpcCallController {

    private final        Environment                  env;
    private final        InfinityProperties           infinityProperties;
    private static final Map<String, ConsumerStub<?>> CONSUMER_STUB_CACHE = new ConcurrentHashMap<>();

    public RpcCallController(Environment env, InfinityProperties infinityProperties) {
        this.env = env;
        this.infinityProperties = infinityProperties;
    }

    @ApiOperation("泛化调用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功调用")})
    @PostMapping("/api/rpc/generic-call")
    public ResponseEntity<Object> genericCall(@ApiParam(value = "调用参数", required = true) @Valid @RequestBody GenericCallDTO dto) {
        ConsumerStub<?> consumerStub = getConsumerStub(dto);
        ProxyFactory proxyFactory = ProxyFactory.getInstance(infinityProperties.getConsumer().getProxyFactory());
        GenericCallHandler genericCallHandler = proxyFactory.createGenericCallHandler(consumerStub);
        Object result = genericCallHandler.call(dto.getMethodName(), dto.getMethodParamTypes(), dto.getArgs(), dto.getOptions());
        return ResponseEntity.ok().body(result);
    }

    private ConsumerStub<?> getConsumerStub(GenericCallDTO dto) {
        String group = StringUtils.defaultIfEmpty(dto.getOptions().get(GROUP), GROUP_VAL_DEFAULT);
        String version = StringUtils.defaultIfEmpty(dto.getOptions().get(VERSION), VERSION_VAL_DEFAULT);
        String key = ProviderStubBeanNameBuilder
                .builder(dto.getInterfaceName(), env)
                .group(group)
                .version(version)
                .build();

        if (CONSUMER_STUB_CACHE.containsKey(key)) {
            return CONSUMER_STUB_CACHE.get(key);
        }
        ConsumerStub<?> consumerStub = new ConsumerStub<>();
        consumerStub.setInterfaceName(dto.getInterfaceName());
        consumerStub.setProtocol(infinityProperties.getAvailableProtocol().getName());
        consumerStub.setCluster(infinityProperties.getConsumer().getCluster());
        consumerStub.setFaultTolerance(infinityProperties.getConsumer().getFaultTolerance());
        consumerStub.setLoadBalancer(infinityProperties.getConsumer().getLoadBalancer());
        consumerStub.setProxyFactory(infinityProperties.getConsumer().getProxyFactory());
        consumerStub.setCheckHealthFactory(infinityProperties.getConsumer().getCheckHealthFactory());

        consumerStub.setGroup(group);
        consumerStub.setVersion(version);
        // must NOT call init
        // consumerStub.init();

        consumerStub.subscribeProviders(infinityProperties.getApplication(),
                infinityProperties.getAvailableProtocol(),
                infinityProperties.getRegistry());

        CONSUMER_STUB_CACHE.put(key, consumerStub);
        return consumerStub;
    }
}
