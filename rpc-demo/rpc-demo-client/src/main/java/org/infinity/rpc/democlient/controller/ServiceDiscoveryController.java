package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.MethodInvocation;
import org.infinity.rpc.core.server.stub.MethodData;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.domain.Application;
import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.dto.RegistryDTO;
import org.infinity.rpc.democlient.service.ApplicationService;
import org.infinity.rpc.democlient.service.ConsumerStubService;
import org.infinity.rpc.democlient.service.ProviderService;
import org.infinity.rpc.democlient.service.RegistryService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.infinity.rpc.core.server.stub.ProviderStub.METHOD_META;
import static org.infinity.rpc.democlient.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Api(tags = "服务发现")
@Slf4j
public class ServiceDiscoveryController {

    @Resource
    private       InfinityProperties  infinityProperties;
    private final RegistryService     registryService;
    private final ProviderService     providerService;
    private final ApplicationService  applicationService;
    private final ConsumerStubService consumerStubService;

    public ServiceDiscoveryController(RegistryService registryService,
                                      ProviderService providerService,
                                      ApplicationService applicationService,
                                      ConsumerStubService consumerStubService) {
        this.registryService = registryService;
        this.providerService = providerService;
        this.applicationService = applicationService;
        this.consumerStubService = consumerStubService;
    }

    @ApiOperation("检索所有注册中心列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("api/service-discovery/registries")
    public ResponseEntity<List<RegistryDTO>> findRegistries() {
        return ResponseEntity.ok(registryService.getRegistries());
    }

    @ApiOperation("检索所有应用列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("api/service-discovery/applications/all")
    public ResponseEntity<List<String>> findApplications(
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "是否活跃") @RequestParam(value = "active", required = false) Boolean active) {
        return ResponseEntity.ok(providerService.findDistinctApplications(registryIdentity, active));
    }

    @ApiOperation("分页检索应用列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("api/service-discovery/applications")
    public ResponseEntity<List<Application>> findApplications(
            Pageable pageable,
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "应用名称(模糊查询)") @RequestParam(value = "name", required = false) String name,
            @ApiParam(value = "是否活跃") @RequestParam(value = "active", required = false) Boolean active) {
        Page<Application> list = applicationService.find(pageable, registryIdentity, name, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("分页检索服务提供者列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("/api/service-discovery/providers")
    public ResponseEntity<List<Provider>> findProviders(
            Pageable pageable,
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "应用名称") @RequestParam(value = "application", required = false) String application,
            @ApiParam(value = "接口名称(模糊查询)") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @ApiParam(value = "是否活跃") @RequestParam(value = "active", required = false) Boolean active) {
        Page<Provider> list = providerService.find(pageable, registryIdentity, application, interfaceName, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("检索服务提供者所有方法")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功执行")})
    @GetMapping("/api/service-discovery/provider/methods")
    public ResponseEntity<List<MethodData>> findMethods(
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "服务提供者URL", required = true) @RequestParam(value = "providerUrl") String providerUrl) {
        ConsumerStub<?> consumerStub = consumerStubService.getConsumerStub(registryIdentity, Url.valueOf(providerUrl));
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        @SuppressWarnings({"unchecked"})
        List<MethodData> result = (List<MethodData>) invocationHandler.invoke(METHOD_META, null, null);
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation("调用服务提供者方法")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功调用")})
    @PostMapping("/api/service-discovery/provider/invoke")
    public ResponseEntity<Object> invoke(
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "服务提供者URL", required = true) @RequestParam(value = "providerUrl") String providerUrl,
            @ApiParam(value = "调用参数", required = true) @RequestBody MethodInvocation data) {
        ConsumerStub<?> consumerStub = consumerStubService.getConsumerStub(registryIdentity, Url.valueOf(providerUrl));
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = invocationHandler.invoke(data.getMethodName(), data.getMethodParamTypes(), data.getArgs());
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation("启用服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功执行")})
    @PutMapping("/api/service-discovery/provider/activate")
    public ResponseEntity<Void> activate(
            @ApiParam(value = "注册中心URL", defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "服务提供者URL") @RequestParam(value = "providerUrl", required = false) String providerUrl) {
        if (StringUtils.isEmpty(registryIdentity)) {
            infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().activate(Url.valueOf(providerUrl)));
        } else {
            registryService.findRegistry(registryIdentity).activate(Url.valueOf(providerUrl));
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ApiOperation("禁用服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功执行")})
    @PutMapping("/api/service-discovery/provider/deactivate")
    public ResponseEntity<Void> deactivate(
            @ApiParam(value = "注册中心URL", defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "服务提供者URL") @RequestParam(value = "providerUrl", required = false) String providerUrl) {
        if (StringUtils.isEmpty(registryIdentity)) {
            infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().deactivate(Url.valueOf(providerUrl)));
        } else {
            registryService.findRegistry(registryIdentity).deactivate(Url.valueOf(providerUrl));
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
