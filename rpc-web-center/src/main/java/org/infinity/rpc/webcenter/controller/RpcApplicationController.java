package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.domain.RpcApplication;
import org.infinity.rpc.webcenter.repository.RpcApplicationRepository;
import org.infinity.rpc.webcenter.service.RpcApplicationService;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class RpcApplicationController {

    @Resource
    private RpcApplicationRepository rpcApplicationRepository;
    @Resource
    private RpcApplicationService    rpcApplicationService;

    @ApiOperation("find all applications")
    @GetMapping("api/rpc-application/applications/all")
    public ResponseEntity<List<String>> findApplications(
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry")
            @RequestParam(value = "registryIdentity") String registryIdentity) {
        List<String> list = rpcApplicationRepository.findByRegistryIdentity(registryIdentity)
                .stream().map(RpcApplication::getName).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @ApiOperation("find application list")
    @GetMapping("api/rpc-application/applications")
    public ResponseEntity<List<RpcApplication>> findApplications(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry")
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "application name(fuzzy query)") @RequestParam(value = "name", required = false) String name,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcApplication> list = rpcApplicationService.find(pageable, registryIdentity, name, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }
}
