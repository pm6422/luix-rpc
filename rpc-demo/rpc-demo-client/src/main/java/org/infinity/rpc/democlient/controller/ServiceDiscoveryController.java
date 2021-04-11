package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.server.stub.MethodData;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.domain.Application;
import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.dto.RegistryDTO;
import org.infinity.rpc.democlient.service.ApplicationService;
import org.infinity.rpc.democlient.service.ProviderService;
import org.infinity.rpc.democlient.service.RegistryService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.infinity.rpc.core.constant.ServiceConstants.*;
import static org.infinity.rpc.core.server.stub.ProviderStub.METHOD_META;
import static org.infinity.rpc.democlient.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Api(tags = "服务发现")
@Slf4j
public class ServiceDiscoveryController {

    @Resource
    private       InfinityProperties infinityProperties;
    private final RegistryService    registryService;
    private final ProviderService    providerService;
    private final ApplicationService applicationService;

    public ServiceDiscoveryController(RegistryService registryService,
                                      ProviderService providerService,
                                      ApplicationService applicationService) {
        this.registryService = registryService;
        this.providerService = providerService;
        this.applicationService = applicationService;
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
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181") @RequestParam(value = "registryUrl") String registryUrl,
            @ApiParam(value = "是否活跃") @RequestParam(value = "active", required = false) Boolean active) {
        return ResponseEntity.ok(providerService.findDistinctApplications(registryUrl, active));
    }

    @ApiOperation("分页检索应用列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("api/service-discovery/applications")
    public ResponseEntity<List<Application>> findApplications(
            Pageable pageable,
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181") @RequestParam(value = "registryUrl") String registryUrl,
            @ApiParam(value = "是否活跃") @RequestParam(value = "active", required = false) Boolean active) {
        Page<Application> list = applicationService.find(pageable, registryUrl, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("分页检索服务提供者列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("/api/service-discovery/providers")
    public ResponseEntity<List<Provider>> findProviders(
            Pageable pageable,
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181") @RequestParam(value = "registryUrl") String registryUrl,
            @ApiParam(value = "应用名称") @RequestParam(value = "application", required = false) String application,
            @ApiParam(value = "接口名称") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @ApiParam(value = "是否活跃") @RequestParam(value = "active", required = false) Boolean active) {
        Page<Provider> list = providerService.find(pageable, registryUrl, application, interfaceName, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("检索服务提供者所有方法")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功执行")})
    @GetMapping("/api/service-discovery/provider/methods")
    public ResponseEntity<List<MethodData>> findMethods(
            @ApiParam(value = "注册中心URL", required = true, defaultValue = "zookeeper://localhost:2181") @RequestParam(value = "registryUrl") String registryUrl,
            @ApiParam(value = "服务提供者URL") @RequestParam(value = "providerUrl", required = false) String providerUrl) {
        Url url = Url.valueOf(providerUrl);
        ConsumerStub<?> consumerStub = ConsumerStub.create(url.getPath(), infinityProperties.getApplication(),
                registryService.findRegistryConfig(registryUrl),
                infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, url.getForm(), url.getVersion(),
                url.getIntOption(REQUEST_TIMEOUT, REQUEST_TIMEOUT_VAL_DEFAULT),
                url.getIntOption(MAX_RETRIES, MAX_RETRIES_VAL_DEFAULT));
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler universalInvocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        @SuppressWarnings({"unchecked"})
        List<MethodData> result = (List<MethodData>) universalInvocationHandler.invoke(METHOD_META, null, null);
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation("启用服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功执行")})
    @PutMapping("/api/service-discovery/provider/activate")
    public ResponseEntity<Void> activate(
            @ApiParam(value = "注册中心URL", defaultValue = "zookeeper://localhost:2181") @RequestParam(value = "registryUrl", required = false) String registryUrl,
            @ApiParam(value = "服务提供者URL") @RequestParam(value = "providerUrl", required = false) String providerUrl) {
        if (StringUtils.isEmpty(registryUrl)) {
            infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().activate(Url.valueOf(providerUrl)));
        } else {
            registryService.findRegistry(registryUrl).activate(Url.valueOf(providerUrl));
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ApiOperation("禁用服务提供者")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功执行")})
    @PutMapping("/api/service-discovery/provider/deactivate")
    public ResponseEntity<Void> deactivate(
            @ApiParam(value = "注册中心URL", defaultValue = "zookeeper://localhost:2181") @RequestParam(value = "registryUrl", required = false) String registryUrl,
            @ApiParam(value = "服务提供者URL") @RequestParam(value = "providerUrl", required = false) String providerUrl) {
        if (StringUtils.isEmpty(registryUrl)) {
            infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().deactivate(Url.valueOf(providerUrl)));
        } else {
            registryService.findRegistry(registryUrl).deactivate(Url.valueOf(providerUrl));
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
