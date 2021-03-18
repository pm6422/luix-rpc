package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.invocationhandler.GenericInvocationHandler;
import org.infinity.rpc.core.client.proxy.ProxyFactory;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.ConsumerStubHolder;
import org.infinity.rpc.democlient.dto.GenericInvokeDTO;
import org.infinity.rpc.spring.boot.bean.name.ConsumerStubBeanNameBuilder;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * REST controller for RPC calling.
 */
@RestController
@Api(tags = "RPC调用")
@Slf4j
public class RpcInvocationController {

    private final Environment        env;
    private final InfinityProperties infinityProperties;

    public RpcInvocationController(Environment env, InfinityProperties infinityProperties) {
        this.env = env;
        this.infinityProperties = infinityProperties;
    }

    /**
      {
      "interfaceName": "org.infinity.rpc.democommon.service.AuthorityService",
      "methodName": "findAll",
      "methodParamTypes": [],
      "args": [],
      "options": {
      "group": "default",
      "version": "1.0.0"
      }
      }
      <p>
      {
      "interfaceName": "org.infinity.rpc.democommon.service.AuthorityService",
      "methodName": "save",
      "methodParamTypes": ["org.infinity.rpc.democommon.domain.Authority"],
      "args": [{
      "name": "ROLE_TEST",
      "enabled": true
      }],
      "options": {
      "group": "default",
      "version": "1.0.0"
      }
      }
     *
     * @param dto dto
     * @return result
     */
    @ApiOperation("泛化调用")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功调用")})
    @PostMapping("/api/rpc/generic-invocation")
    public ResponseEntity<Object> genericInvoke(@ApiParam(value = "调用参数", required = true) @Valid @RequestBody GenericInvokeDTO dto) {
        ConsumerStub<?> consumerStub = getConsumerStub(dto);
        ProxyFactory proxyFactory = ProxyFactory.getInstance(infinityProperties.getConsumer().getProxyFactory());
        GenericInvocationHandler genericInvocationHandler = proxyFactory.createGenericInvokeHandler(consumerStub);
        Object result = genericInvocationHandler.genericInvoke(dto.getMethodName(), dto.getMethodParamTypes(), dto.getArgs(), dto.getOptions());
        return ResponseEntity.ok().body(result);
    }

    private ConsumerStub<?> getConsumerStub(GenericInvokeDTO dto) {
        Map<String, Object> optionMap = new HashMap<>(dto.getOptions());
        for (Map.Entry<String, String> entry : dto.getOptions().entrySet()) {
            optionMap.put(entry.getKey(), entry.getValue());
        }
        String beanName = ConsumerStubBeanNameBuilder
                .builder(dto.getInterfaceName(), env)
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
        consumerStub.setProxyFactory(infinityProperties.getConsumer().getProxyFactory());
        consumerStub.setCheckHealthFactory(infinityProperties.getConsumer().getCheckHealthFactory());
        // Must NOT call init()

        consumerStub.subscribeProviders(infinityProperties.getApplication(),
                infinityProperties.getAvailableProtocol(),
                infinityProperties.getRegistry());

        ConsumerStubHolder.getInstance().addStub(beanName, consumerStub);
        return consumerStub;
    }
}
