package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.server.buildin.BuildInService;
import org.infinity.rpc.core.server.stub.MethodData;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.domain.Application;
import org.infinity.rpc.democlient.domain.Provider;
import org.infinity.rpc.democlient.dto.MethodInvocation;
import org.infinity.rpc.democlient.dto.RegistryDTO;
import org.infinity.rpc.democlient.service.ApplicationService;
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

import static org.infinity.rpc.core.server.buildin.BuildInService.METHOD_GET_HEALTH;
import static org.infinity.rpc.core.server.stub.ProviderStub.METHOD_META;
import static org.infinity.rpc.democlient.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class ServiceDiscoveryController {

    @Resource
    private InfinityProperties infinityProperties;
    @Resource
    private RegistryService    registryService;
    @Resource
    private ProviderService    providerService;
    @Resource
    private ApplicationService applicationService;

    @ApiOperation("find all registries")
    @GetMapping("api/service-discovery/registries")
    public ResponseEntity<List<RegistryDTO>> findRegistries() {
        return ResponseEntity.ok(registryService.getRegistries());
    }

    @ApiOperation("find all applications")
    @GetMapping("api/service-discovery/applications/all")
    public ResponseEntity<List<String>> findApplications(
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        return ResponseEntity.ok(providerService.findDistinctApplications(registryIdentity, active));
    }

    @ApiOperation("find application list")
    @GetMapping("api/service-discovery/applications")
    public ResponseEntity<List<Application>> findApplications(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "application name(fuzzy query)") @RequestParam(value = "name", required = false) String name,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<Application> list = applicationService.find(pageable, registryIdentity, name, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("find provider list")
    @GetMapping("/api/service-discovery/providers")
    public ResponseEntity<List<Provider>> findProviders(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "application name") @RequestParam(value = "application", required = false) String application,
            @ApiParam(value = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<Provider> list = providerService.find(pageable, registryIdentity, application, interfaceName, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("find all methods of provider")
    @GetMapping("/api/service-discovery/provider/methods")
    public ResponseEntity<List<MethodData>> findMethods(
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrl) {
        ConsumerStub<?> consumerStub = registryService.getConsumerStub(registryIdentity, Url.valueOf(providerUrl));
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        @SuppressWarnings({"unchecked"})
        List<MethodData> result = (List<MethodData>) invocationHandler.invoke(METHOD_META, null, null);
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation("invoke provider method")
    @PostMapping("/api/service-discovery/provider/invoke")
    public ResponseEntity<Object> invoke(
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrl,
            @ApiParam(value = "argument", required = true) @RequestBody MethodInvocation data) {
        ConsumerStub<?> consumerStub = registryService.getConsumerStub(registryIdentity, Url.valueOf(providerUrl));
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Object result = invocationHandler.invoke(data.getMethodName(), data.getMethodParamTypes(), data.getArgs());
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation(value = "check health of provider", notes = "There is no service discovery in the direct connection mode, even the inactive provider can be called successfully")
    @GetMapping("/api/service-discovery/provider/health")
    public ResponseEntity<String> health(@ApiParam(value = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrl) {
        Url url = Url.valueOf(providerUrl);
        ConsumerStub<?> consumerStub = ConsumerStub.create(BuildInService.class.getName(), infinityProperties.getApplication(),
                infinityProperties.getRegistry(), infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, url.getAddress(), null, null, null, null);
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        String result;
        try {
            result = (String) invocationHandler.invoke(METHOD_GET_HEALTH, null, null);
        } catch (Exception ex) {
            result = ex.getMessage();
        }
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation("activate provider")
    @PutMapping("/api/service-discovery/provider/activate")
    public ResponseEntity<Void> activate(
            @ApiParam(value = "registry url identity", defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrl) {
        if (StringUtils.isEmpty(registryIdentity)) {
            infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().activate(Url.valueOf(providerUrl)));
        } else {
            registryService.findRegistry(registryIdentity).activate(Url.valueOf(providerUrl));
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ApiOperation("deactivate provider")
    @PutMapping("/api/service-discovery/provider/deactivate")
    public ResponseEntity<Void> deactivate(
            @ApiParam(value = "registry url identity", defaultValue = "zookeeper://localhost:2181/registry") @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrl) {
        if (StringUtils.isEmpty(registryIdentity)) {
            infinityProperties.getRegistryList().forEach(config -> config.getRegistryImpl().deactivate(Url.valueOf(providerUrl)));
        } else {
            registryService.findRegistry(registryIdentity).deactivate(Url.valueOf(providerUrl));
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
