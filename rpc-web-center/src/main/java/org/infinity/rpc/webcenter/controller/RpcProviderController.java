package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.stub.MethodData;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.infinity.rpc.webcenter.dto.MethodInvocation;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcProviderRepository;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.infinity.rpc.webcenter.service.RpcRegistryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static org.infinity.rpc.core.server.buildin.BuildInService.METHOD_GET_HEALTH;
import static org.infinity.rpc.core.server.buildin.BuildInService.METHOD_GET_METHODS;
import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class RpcProviderController {

    @Resource
    private InfinityProperties    infinityProperties;
    @Resource
    private RpcRegistryService    rpcRegistryService;
    @Resource
    private RpcProviderService    rpcProviderService;
    @Resource
    private RpcProviderRepository rpcProviderRepository;

    @ApiOperation("find provider by ID")
    @GetMapping("/api/rpc-provider/{id}")
    public ResponseEntity<RpcProvider> findById(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        RpcProvider domain = rpcProviderRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("find provider list")
    @GetMapping("/api/rpc-provider/providers")
    public ResponseEntity<List<RpcProvider>> findProviders(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "application name") @RequestParam(value = "application", required = false) String application,
            @ApiParam(value = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcProvider> list = rpcProviderService.find(pageable, registryIdentity, application, interfaceName, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("find all methods of provider")
    @GetMapping("/api/rpc-provider/methods")
    public ResponseEntity<List<MethodData>> findMethods(
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        ConsumerStub<?> consumerStub = ConsumerStub.create(BuildInService.class.getName(),
                infinityProperties.getApplication(), rpcRegistryService.findRegistryConfig(registryIdentity),
                infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, providerUrl.getAddress(), null, null, 10000, null);

        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        @SuppressWarnings({"unchecked"})
        List<MethodData> result = (List<MethodData>) invocationHandler.invoke(METHOD_GET_METHODS,
                new String[]{String.class.getName(), String.class.getName(), String.class.getName()},
                new Object[]{providerUrl.getPath(), providerUrl.getForm(), providerUrl.getVersion()});
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation("invoke provider method")
    @PostMapping("/api/rpc-provider/invoke")
    public ResponseEntity<Object> invoke(@ApiParam(value = "methodInvocation", required = true) @Valid @RequestBody MethodInvocation methodInvocation) {
        ConsumerStub<?> consumerStub = rpcRegistryService.getConsumerStub(methodInvocation.getRegistryIdentity(), Url.valueOf(methodInvocation.getProviderUrl()));
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = invocationHandler.invoke(methodInvocation.getMethodName(), methodInvocation.getMethodParamTypes(), methodInvocation.getArgs());
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation(value = "check health of provider", notes = "There is no service discovery in the direct connection mode, even the inactive provider can be called successfully")
    @GetMapping("/api/rpc-provider/health")
    public ResponseEntity<String> health(@ApiParam(value = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrl) {
        Url url = Url.valueOf(providerUrl);
        ConsumerStub<?> consumerStub = ConsumerStub.create(BuildInService.class.getName(), infinityProperties.getApplication(),
                infinityProperties.getRegistry(), infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, url.getAddress(), null, null, null, null);
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        String result;
        try {
            result = (String) invocationHandler.invoke(METHOD_GET_HEALTH);
        } catch (Exception ex) {
            result = ex.getMessage();
        }
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation("activate provider")
    @PutMapping("/api/rpc-provider/activate")
    public ResponseEntity<Void> activate(
            @ApiParam(value = "registry url identity", defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrl) {
        if (StringUtils.isEmpty(registryIdentity)) {
            infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().activate(Url.valueOf(providerUrl)));
        } else {
            rpcRegistryService.findRegistry(registryIdentity).activate(Url.valueOf(providerUrl));
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ApiOperation("deactivate provider")
    @PutMapping("/api/rpc-provider/deactivate")
    public ResponseEntity<Void> deactivate(
            @ApiParam(value = "registry url identity", defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrl) {
        if (StringUtils.isEmpty(registryIdentity)) {
            infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().deactivate(Url.valueOf(providerUrl)));
        } else {
            rpcRegistryService.findRegistry(registryIdentity).deactivate(Url.valueOf(providerUrl));
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
