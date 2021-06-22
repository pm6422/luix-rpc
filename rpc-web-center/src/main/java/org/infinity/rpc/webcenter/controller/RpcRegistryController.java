package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.dto.RegistryDTO;
import org.infinity.rpc.webcenter.service.RegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Slf4j
public class RpcRegistryController {

    @Resource
    private RegistryService registryService;

    @ApiOperation("find all registries")
    @GetMapping("api/rpc-registry/registries")
    public ResponseEntity<List<RegistryDTO>> findRegistries() {
        return ResponseEntity.ok(registryService.getRegistries());
    }
}
