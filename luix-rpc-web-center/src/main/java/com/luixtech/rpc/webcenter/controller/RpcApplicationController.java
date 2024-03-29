package com.luixtech.rpc.webcenter.controller;

import com.luixtech.rpc.webcenter.domain.RpcApplication;
import com.luixtech.rpc.webcenter.repository.RpcApplicationRepository;
import com.luixtech.rpc.webcenter.service.RpcApplicationService;
import com.luixtech.rpc.webcenter.service.RpcConsumerService;
import com.luixtech.rpc.webcenter.service.RpcProviderService;
import com.luixtech.rpc.webcenter.utils.HttpHeaderUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static com.luixtech.rpc.webcenter.LuixRpcWebCenterApplication.DEFAULT_REG;


@RestController
@AllArgsConstructor
@Slf4j
public class RpcApplicationController {
    private final RpcApplicationRepository rpcApplicationRepository;
    private final RpcApplicationService    rpcApplicationService;
    private final RpcProviderService       rpcProviderService;
    private final RpcConsumerService       rpcConsumerService;

    @Operation(summary = "find all application names")
    @GetMapping("api/rpc-applications/names")
    public ResponseEntity<List<String>> findApplications(
            @Parameter(description = "registry url identity", required = true, schema = @Schema(defaultValue = DEFAULT_REG))
            @RequestParam(value = "registryIdentity") String registryIdentity) {
        List<String> results = rpcApplicationRepository.findByRegistryIdentity(registryIdentity)
                .stream().map(RpcApplication::getId).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "find application list")
    @GetMapping("api/rpc-applications")
    public ResponseEntity<List<RpcApplication>> findApplications(
            @ParameterObject Pageable pageable,
            @Parameter(description = "registry url identity", required = true, schema = @Schema(defaultValue = DEFAULT_REG))
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @Parameter(description = "application name(fuzzy query)") @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcApplication> results = rpcApplicationService.find(pageable, registryIdentity, name, active);
        if (!results.isEmpty()) {
            results.getContent().forEach(domain -> {
                if (rpcProviderService.existsApplication(registryIdentity, domain.getId(), true)) {
                    domain.setProviding(true);
                } else {
                    domain.setProviding(false);
                }
                if (rpcConsumerService.existsApplication(registryIdentity, domain.getId(), true)) {
                    domain.setConsuming(true);
                } else {
                    domain.setConsuming(false);
                }
                if (domain.isProviding() || domain.isConsuming()) {
                    domain.setActive(true);
                } else {
                    domain.setActive(false);
                }
            });
        }
        return ResponseEntity.ok().headers(HttpHeaderUtils.generatePageHeaders(results)).body(results.getContent());
    }
}
