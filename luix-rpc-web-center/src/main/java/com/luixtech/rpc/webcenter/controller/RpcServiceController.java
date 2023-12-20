package com.luixtech.rpc.webcenter.controller;

import com.luixtech.rpc.webcenter.domain.RpcService;
import com.luixtech.rpc.webcenter.dto.InterfaceActivateDTO;
import com.luixtech.rpc.webcenter.dto.ProviderActivateDTO;
import com.luixtech.rpc.webcenter.exception.DataNotFoundException;
import com.luixtech.rpc.webcenter.repository.RpcServiceRepository;
import com.luixtech.rpc.webcenter.service.RpcConsumerService;
import com.luixtech.rpc.webcenter.service.RpcProviderService;
import com.luixtech.rpc.webcenter.service.RpcServiceService;
import com.luixtech.springbootframework.component.HttpHeaderCreator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.luixtech.rpc.webcenter.LuixRpcWebCenterApplication.DEFAULT_REG;
import static com.luixtech.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@AllArgsConstructor
@Slf4j
public class RpcServiceController {
    private final RpcServiceService     rpcServiceService;
    private final RpcServiceRepository  rpcServiceRepository;
    private final RpcProviderService    rpcProviderService;
    private final RpcConsumerService    rpcConsumerService;
    private final RpcProviderController rpcProviderController;
    private final HttpHeaderCreator     httpHeaderCreator;

    @Operation(summary = "find service by ID")
    @GetMapping("/api/rpc-services/{id}")
    public ResponseEntity<RpcService> findById(@Parameter(description = "ID", required = true) @PathVariable String id) {
        RpcService domain = rpcServiceRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        if (rpcProviderService.existsService(domain.getRegistryIdentity(), domain.getInterfaceName(), true)) {
            domain.setProviding(true);
            domain.setActive(true);
        }
        if (rpcConsumerService.existsService(domain.getRegistryIdentity(), domain.getInterfaceName(), true)) {
            domain.setConsuming(true);
            domain.setActive(true);
        }
        return ResponseEntity.ok(domain);
    }

    @Operation(summary = "find service")
    @GetMapping("/api/rpc-services/service")
    public ResponseEntity<RpcService> find(
            @Parameter(description = "registry url identity", required = true, schema = @Schema(defaultValue = DEFAULT_REG))
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @Parameter(description = "interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        String id = DigestUtils.md5DigestAsHex((interfaceName + "@" + registryIdentity).getBytes());
        RpcService domain = rpcServiceRepository.findById(id).orElseThrow(() -> new DataNotFoundException(interfaceName));
        if (rpcProviderService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
            domain.setProviding(true);
            domain.setActive(true);
        }
        if (rpcConsumerService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
            domain.setConsuming(true);
            domain.setActive(true);
        }
        return ResponseEntity.ok(domain);
    }

    @Operation(summary = "find service list")
    @GetMapping("/api/rpc-services")
    public ResponseEntity<List<RpcService>> findRpcServices(
            @ParameterObject Pageable pageable,
            @Parameter(description = "registry url identity", required = true, schema = @Schema(defaultValue = DEFAULT_REG))
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @Parameter(description = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        Page<RpcService> results = rpcServiceService.find(pageable, registryIdentity, interfaceName);
        if (!results.isEmpty()) {
            results.getContent().forEach(domain -> {
                if (rpcProviderService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
                    domain.setProviding(true);
                    domain.setActive(true);
                }
                if (rpcConsumerService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
                    domain.setConsuming(true);
                    domain.setActive(true);
                }
            });
        }
        return ResponseEntity.ok().headers(generatePageHeaders(results)).body(results.getContent());
    }

    @Operation(summary = "activate all providers of the interface")
    @PutMapping("/api/rpc-services/activate")
    public ResponseEntity<Void> activate(@Valid @RequestBody InterfaceActivateDTO activateDTO) {
        rpcProviderService.find(activateDTO.getRegistryIdentity(), activateDTO.getInterfaceName(), false)
                .forEach(provider -> rpcProviderController.activate(
                        ProviderActivateDTO.builder()
                                .registryIdentity(activateDTO.getRegistryIdentity())
                                .providerUrl(provider.getUrl())
                                .build()));
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }

    @Operation(summary = "deactivate all providers of the interface")
    @PutMapping("/api/rpc-services/deactivate")
    public ResponseEntity<Void> deactivate(@Valid @RequestBody InterfaceActivateDTO activateDTO) {
        rpcProviderService.find(activateDTO.getRegistryIdentity(), activateDTO.getInterfaceName(), true)
                .forEach(provider -> rpcProviderController.deactivate(
                        ProviderActivateDTO.builder()
                                .registryIdentity(activateDTO.getRegistryIdentity())
                                .providerUrl(provider.getUrl())
                                .build()));
        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaderCreator.createSuccessHeader("SM1012")).build();
    }
}
