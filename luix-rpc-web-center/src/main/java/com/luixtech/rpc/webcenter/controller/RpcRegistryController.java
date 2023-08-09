package com.luixtech.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import com.luixtech.rpc.webcenter.dto.RpcRegistryDTO;
import com.luixtech.rpc.webcenter.service.RpcRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.luixtech.springbootframework.config.api.SpringDocConfiguration.AUTH;


@RestController
@SecurityRequirement(name = AUTH)
@AllArgsConstructor
@Slf4j
public class RpcRegistryController {
    private final RpcRegistryService rpcRegistryService;

    @Operation(summary = "find all registries")
    @GetMapping("open-api/rpc-registries")
    @Timed
    public ResponseEntity<List<RpcRegistryDTO>> findRegistries() {
        return ResponseEntity.ok(rpcRegistryService.getRegistries());
    }
}
