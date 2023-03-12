package com.luixtech.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import com.luixtech.framework.component.HttpHeaderCreator;
import com.luixtech.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import com.luixtech.rpc.core.client.proxy.Proxy;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.core.client.stub.ConsumerStubFactory;
import com.luixtech.rpc.core.server.stub.MethodMeta;
import com.luixtech.rpc.core.server.stub.ProviderStub;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.spring.boot.starter.config.LuixRpcProperties;
import com.luixtech.rpc.webcenter.domain.Authority;
import com.luixtech.rpc.webcenter.domain.RpcProvider;
import com.luixtech.rpc.webcenter.dto.OptionMetaDTO;
import com.luixtech.rpc.webcenter.dto.OptionsDTO;
import com.luixtech.rpc.webcenter.dto.ProviderActivateDTO;
import com.luixtech.rpc.webcenter.exception.DataNotFoundException;
import com.luixtech.rpc.webcenter.repository.RpcProviderRepository;
import com.luixtech.rpc.webcenter.service.RpcProviderService;
import com.luixtech.rpc.webcenter.service.RpcRegistryService;
import com.luixtech.rpc.webcenter.utils.HttpHeaderUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.luixtech.framework.config.api.SpringDocConfiguration.AUTH;
import static com.luixtech.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_BROADCAST;
import static com.luixtech.rpc.core.server.stub.ProviderStub.*;
import static com.luixtech.rpc.serializer.Serializer.SERIALIZER_NAME_HESSIAN2;
import static com.luixtech.rpc.webcenter.LuixRpcWebCenterApplication.DEFAULT_REG;

@RestController
@SecurityRequirement(name = AUTH)
@AllArgsConstructor
@Slf4j
public class RpcProviderController {
    private final LuixRpcProperties  luixRpcProperties;
    private final RpcRegistryService rpcRegistryService;
    private final RpcProviderService    rpcProviderService;
    private final RpcProviderRepository rpcProviderRepository;
    private final HttpHeaderCreator     httpHeaderCreator;

    @Operation(summary = "find provider by ID")
    @GetMapping("/api/rpc-providers/{id}")
    @Timed
    public ResponseEntity<RpcProvider> findById(@Parameter(description = "ID", required = true) @PathVariable String id) {
        RpcProvider domain = rpcProviderRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @Operation(summary = "find provider list")
    @GetMapping("/api/rpc-providers")
    @Timed
    public ResponseEntity<List<RpcProvider>> findProviders(
            @ParameterObject Pageable pageable,
            @Parameter(description = "registry url identity", required = true, schema = @Schema(defaultValue = DEFAULT_REG)) @RequestParam(value = "registryIdentity") String registryIdentity,
            @Parameter(description = "application name") @RequestParam(value = "application", required = false) String application,
            @Parameter(description = "address") @RequestParam(value = "address", required = false) String address,
            @Parameter(description = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @Parameter(description = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcProvider> list = rpcProviderService.find(pageable, registryIdentity, application, address, interfaceName, active);
        return ResponseEntity.ok().headers(HttpHeaderUtils.generatePageHeaders(list)).body(list.getContent());
    }

    @Operation(summary = "find all methods of provider")
    @GetMapping("/api/rpc-providers/methods")
    @Timed
    public ResponseEntity<List<MethodMeta>> findMethods(
            @Parameter(description = "registry url identity", required = true, schema = @Schema(defaultValue = DEFAULT_REG)) @RequestParam(value = "registryIdentity") String registryIdentity,
            @Parameter(description = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrlStr) {
        Url providerUrl = Url.valueOf(providerUrlStr);
        // Use specified provider url
        UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(registryIdentity, providerUrl);
        @SuppressWarnings({"unchecked"})
        List<MethodMeta> result = (List<MethodMeta>) invocationHandler.invoke(METHOD_GET_METHOD_METAS);
        return ResponseEntity.ok().body(result);
    }

    @Operation(summary = "check health of provider", description = "There is no service discovery in the direct connection mode, even the inactive provider can be called successfully")
    @GetMapping("/api/rpc-providers/health")
    @Timed
    public ResponseEntity<String> health(
            @Parameter(description = "registry url identity", required = true, schema = @Schema(defaultValue = DEFAULT_REG)) @RequestParam(value = "registryIdentity") String registryIdentity,
            @Parameter(description = "provider url", required = true) @RequestParam(value = "providerUrl") String providerUrlStr) {
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
                luixRpcProperties.getApplication(), rpcRegistryService.findRegistryConfig(registryIdentity),
                luixRpcProperties.getAvailableProtocol(), providerUrl.getAddress(), providerUrl.getPath(),
                providerUrl.getForm(), providerUrl.getVersion());
        Proxy proxyFactory = Proxy.getInstance(luixRpcProperties.getConsumer().getProxyFactory());
        return proxyFactory.createUniversalInvocationHandler(consumerStub);
    }

    @Operation(summary = "activate provider")
    @PutMapping("/api/rpc-providers/activate")
    @PreAuthorize("hasAuthority(\"" + Authority.ADMIN + "\")")
    @Timed
    public ResponseEntity<Void> activate(@Valid @RequestBody ProviderActivateDTO activateDTO) {
        Url providerUrl = Url.valueOf(activateDTO.getProviderUrl());
        control(activateDTO.getRegistryIdentity(), providerUrl, METHOD_ACTIVATE);
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }

    @Operation(summary = "deactivate provider")
    @PutMapping("/api/rpc-providers/deactivate")
    @PreAuthorize("hasAuthority(\"" + Authority.ADMIN + "\")")
    @Timed
    public ResponseEntity<Void> deactivate(@Valid @RequestBody ProviderActivateDTO activateDTO) {
        Url providerUrl = Url.valueOf(activateDTO.getProviderUrl());
        control(activateDTO.getRegistryIdentity(), providerUrl, METHOD_DEACTIVATE);
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }

    private void control(String registryIdentity, Url providerUrl, String methodName) {
        if (StringUtils.isEmpty(registryIdentity)) {
            luixRpcProperties.getRegistryList().forEach(registry -> {
                String identity = registry.getRegistryImpl().getRegistryUrl().getIdentity();
                // Use specified provider url
                UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(identity, providerUrl);
                invocationHandler.invoke(methodName);
                if (methodName.equals(METHOD_ACTIVATE)) {
                    rpcProviderService.updateActiveByRegistryIdentityAndUrl(true, identity, providerUrl.toFullStr());
                } else {
                    rpcProviderService.updateActiveByRegistryIdentityAndUrl(false, identity, providerUrl.toFullStr());
                }
            });
        } else {
            // Use specified provider url
            UniversalInvocationHandler invocationHandler = createBuildInInvocationHandler(registryIdentity, providerUrl);
            invocationHandler.invoke(methodName);
            if (methodName.equals(METHOD_ACTIVATE)) {
                rpcProviderService.updateActiveByRegistryIdentityAndUrl(true, registryIdentity, providerUrl.toFullStr());
            } else {
                rpcProviderService.updateActiveByRegistryIdentityAndUrl(false, registryIdentity, providerUrl.toFullStr());
            }
        }
    }

    @Operation(summary = "get provider options")
    @GetMapping("/api/rpc-providers/options")
    public List<OptionMetaDTO> options(
            @Parameter(description = "provider url") @RequestParam(value = "providerUrl", required = false) String providerUrlStr) {
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

    @Operation(summary = "save options")
    @PutMapping("/api/rpc-providers/options")
    @PreAuthorize("hasAuthority(\"" + Authority.ADMIN + "\")")
    @Timed
    public ResponseEntity<Void> saveOptions(@Parameter(description = "optionsDTO", required = true)
                                            @Valid @RequestBody OptionsDTO optionsDTO) {
        Url providerUrl = Url.valueOf(optionsDTO.getUrl());
        for (OptionMetaDTO next : optionsDTO.getOptions()) {
            if (StringUtils.isEmpty(next.getValue()) || next.getDefaultValue().equals(next.getValue())) {
                providerUrl.getOptions().remove(next.getName());
            } else if (next.getType().equals("Integer")) {
                providerUrl.addOption(next.getName(), next.getValue());
            }
        }

        reregister(optionsDTO.getRegistryIdentity(), providerUrl);
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }

    private void reregister(String registryIdentity, Url providerUrl) {
        if (StringUtils.isEmpty(registryIdentity)) {
            luixRpcProperties.getRegistryList().forEach(registry -> {
                doReregister(registry.getRegistryImpl().getRegistryUrl().getIdentity(), providerUrl);
            });
        } else {
            doReregister(registryIdentity, providerUrl);
        }
    }

    private void doReregister(String registryIdentity, Url providerUrl) {
        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(luixRpcProperties.getApplication(),
                rpcRegistryService.findRegistryConfig(registryIdentity),
                luixRpcProperties.getAvailableProtocol(),
                providerUrl.getPath(), SERIALIZER_NAME_HESSIAN2, providerUrl.getForm(), providerUrl.getVersion(),
                60 * 1000, FAULT_TOLERANCE_VAL_BROADCAST);

        Proxy proxyFactory = Proxy.getInstance(luixRpcProperties.getConsumer().getProxyFactory());
        UniversalInvocationHandler invocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        invocationHandler.invoke(METHOD_REREGISTER, new String[]{Map.class.getName()}, new Object[]{providerUrl.getOptions()});
    }
}
