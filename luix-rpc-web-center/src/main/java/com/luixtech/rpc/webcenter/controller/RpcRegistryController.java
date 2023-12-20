package com.luixtech.rpc.webcenter.controller;

import com.luixtech.rpc.webcenter.dto.RpcRegistryDTO;
import com.luixtech.rpc.webcenter.service.RpcRegistryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@AllArgsConstructor
@Slf4j
public class RpcRegistryController {
    private final RpcRegistryService rpcRegistryService;

    @Operation(summary = "find all registries")
    @GetMapping("open-api/rpc-registries")
    public ResponseEntity<List<RpcRegistryDTO>> findRegistries() {
        return ResponseEntity.ok(rpcRegistryService.getRegistries());
    }
}
