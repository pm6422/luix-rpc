package org.infinity.rpc.demoserver.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.invocationhandler.GenericCallHandler;
import org.infinity.rpc.core.client.proxy.ProxyFactory;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.demoserver.dto.GenericCallDTO;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.infinity.rpc.core.constant.ServiceConstants.GROUP;
import static org.infinity.rpc.core.constant.ServiceConstants.VERSION;

/**
 * REST controller for RPC calling.
 */
@RestController
@Api(tags = "RPC调用")
@Slf4j
public class RpcCallController {

    private final InfinityProperties infinityProperties;

    public RpcCallController(InfinityProperties infinityProperties) {
        this.infinityProperties = infinityProperties;
    }

    @ApiOperation("泛化调用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功调用")})
    @PostMapping("/api/rpc/generic-call")
    public ResponseEntity<Object> genericCall(@ApiParam(value = "调用参数", required = true) @Valid @RequestBody GenericCallDTO dto) {
        ConsumerStub<?> consumerStub = new ConsumerStub<>();
        consumerStub.setInterfaceName(dto.getInterfaceName());
        consumerStub.setProtocol(infinityProperties.getAvailableProtocol().getName());
        consumerStub.setCluster(infinityProperties.getConsumer().getCluster());
        consumerStub.setFaultTolerance(infinityProperties.getConsumer().getFaultTolerance());
        consumerStub.setLoadBalancer(infinityProperties.getConsumer().getLoadBalancer());
        consumerStub.setProxyFactory(infinityProperties.getConsumer().getProxyFactory());
        consumerStub.setCheckHealthFactory(infinityProperties.getConsumer().getCheckHealthFactory());

        consumerStub.setGroup(dto.getOptions().get(GROUP));
        consumerStub.setVersion(dto.getOptions().get(VERSION));
        consumerStub.init();

        consumerStub.subscribeProviders(infinityProperties.getApplication(),
                infinityProperties.getAvailableProtocol(),
                infinityProperties.getRegistry());

        ProxyFactory proxyFactory = ProxyFactory.getInstance(infinityProperties.getConsumer().getProxyFactory());
        GenericCallHandler genericCallHandler = proxyFactory.createGenericCallHandler(consumerStub);
        Object result = genericCallHandler.genericCall(dto.getMethodName(), dto.getMethodParamTypes(), dto.getArgs(), dto.getOptions());
        return ResponseEntity.ok().body(result);
    }
}
