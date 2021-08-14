package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.ConsumerStubFactory;
import org.infinity.rpc.core.server.stub.MethodMeta;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.webcenter.component.HttpHeaderCreator;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.domain.RpcProvider;
import org.infinity.rpc.webcenter.dto.OptionMetaDTO;
import org.infinity.rpc.webcenter.dto.OptionsDTO;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcProviderRepository;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.infinity.rpc.webcenter.service.RpcRegistryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.infinity.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_BROADCAST;
import static org.infinity.rpc.core.server.stub.ProviderStub.*;
import static org.infinity.rpc.utilities.serializer.Serializer.SERIALIZER_NAME_HESSIAN2;
import static org.infinity.rpc.webcenter.config.ApplicationConstants.DEFAULT_REG;
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
    @Resource
    private HttpHeaderCreator     httpHeaderCreator;

    @ApiOperation("find provider by ID")
    @GetMapping("/api/rpc-providers/{id}")
    public ResponseEntity<RpcProvider> findById(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        RpcProvider domain = rpcProviderRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("find provider list")
    @GetMapping("/api/rpc-providers")
    public ResponseEntity<List<RpcProvider>> findProviders(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "application name") @RequestParam(value = "application", required = false) String application,
            @ApiParam(value = "address") @RequestParam(value = "address", required = false) String address,
            @ApiParam(value = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcProvider> list = rpcProviderService.find(pageable, registryIdentity, application, address, interfaceName, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("find all methods of provider")
    @GetMapping("/api/rpc-providers/methods")
    public ResponseEntity<List<MethodMeta>> findMethods(
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        // Use specified provider url
        UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(registryIdentity, providerUrl);
        @SuppressWarnings({"unchecked"})
        List<MethodMeta> result = (List<MethodMeta>) invocationHandler.invoke(METHOD_GET_METHOD_METAS);
        return ResponseEntity.ok().body(result);
    }

    @ApiOperation(value = "check health of provider", notes = "There is no service discovery in the direct connection mode, even the inactive provider can be called successfully")
    @GetMapping("/api/rpc-providers/health")
    public ResponseEntity<String> health(
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        // Use specified provider url
        UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(registryIdentity, providerUrl);
        String result;
        try {
            result = (String) invocationHandler.invoke(METHOD_CHECK_HEALTH);
        } catch (Exception ex) {
            result = ex.getMessage();
        }
        return ResponseEntity.ok().body(result);
    }

    private UniversalInvocationHandler createBuildInInvocationHandler(String registryIdentity, Url providerUrl) {
        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(
                infinityProperties.getApplication(), rpcRegistryService.findRegistryConfig(registryIdentity),
                infinityProperties.getAvailableProtocol(), providerUrl.getAddress(), providerUrl.getPath());
        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        return proxyFactory.createUniversalInvocationHandler(consumerStub);
    }

    @ApiOperation("activate provider")
    @GetMapping("/api/rpc-providers/activate")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> activate(
            @ApiParam(value = "registry url identity", defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        control(registryIdentity, providerUrl, METHOD_ACTIVATE);
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }

    @ApiOperation("deactivate provider")
    @GetMapping("/api/rpc-providers/deactivate")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> deactivate(
            @ApiParam(value = "registry url identity", defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity", required = false) String registryIdentity,
            @ApiParam(value = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        control(registryIdentity, providerUrl, METHOD_DEACTIVATE);
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }

    private void control(String registryIdentity, Url providerUrl, String methodName) {
        if (StringUtils.isEmpty(registryIdentity)) {
            infinityProperties.getRegistryList().forEach(registry -> {
                String identity = registry.getRegistryImpl().getRegistryUrl().getIdentity();
                // Use specified provider url
                UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(identity, providerUrl);
                invocationHandler.invoke(methodName);
            });
        } else {
            // Use specified provider url
            UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(registryIdentity, providerUrl);
            invocationHandler.invoke(methodName);
        }
    }

    @ApiOperation("get provider options")
    @GetMapping("/api/rpc-providers/options")
    public List<OptionMetaDTO> options(
            @ApiParam(value = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        Map<String, String> options = providerUrl.getOptions();

        List<OptionMetaDTO> all = ProviderStub.OPTIONS.stream().map(OptionMetaDTO::of).collect(Collectors.toList());
        all.forEach(dto -> {
            if (dto.getType().equals(Boolean.class.getSimpleName())) {
                dto.setValue(dto.getDefaultValue());
            }
            if (options.containsKey(dto.getName())) {
                dto.setValue(options.get(dto.getName()));
            }
        });
        return all;
    }

    @ApiOperation("save options")
    @PutMapping("/api/rpc-providers/options")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> saveOptions(@ApiParam(value = "optionsDTO", required = true)
                                            @Valid @RequestBody OptionsDTO optionsDTO) {
        Url providerUrl = Url.valueOf(optionsDTO.getUrl());
        for (OptionMetaDTO next : optionsDTO.getOptions()) {
            if (StringUtils.isEmpty(next.getValue()) || next.getDefaultValue().equals(next.getValue())) {
                providerUrl.getOptions().remove(next.getName());
            } else {
                providerUrl.addOption(next.getName(), next.getValue());
            }
        }

        reregister(optionsDTO.getRegistryIdentity(), providerUrl);
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }

    private void reregister(String registryIdentity, Url providerUrl) {
        if (StringUtils.isEmpty(registryIdentity)) {
            infinityProperties.getRegistryList().forEach(registry -> {
                doReregister(registry.getRegistryImpl().getRegistryUrl().getIdentity(), providerUrl);
            });
        } else {
            doReregister(registryIdentity, providerUrl);
        }
    }

    private void doReregister(String registryIdentity, Url providerUrl) {
        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(infinityProperties.getApplication(),
                rpcRegistryService.findRegistryConfig(registryIdentity),
                infinityProperties.getAvailableProtocol(),
                providerUrl.getPath(), SERIALIZER_NAME_HESSIAN2, providerUrl.getForm(), providerUrl.getVersion(), FAULT_TOLERANCE_VAL_BROADCAST);

        Proxy proxyFactory = Proxy.getInstance(infinityProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        invocationHandler.invoke(METHOD_REREGISTER, new String[]{Map.class.getName()}, new Object[]{providerUrl.getOptions()});
    }
}
