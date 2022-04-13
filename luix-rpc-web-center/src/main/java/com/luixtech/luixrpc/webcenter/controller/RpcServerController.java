package com.luixtech.luixrpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.webcenter.config.ApplicationConstants;
import com.luixtech.luixrpc.webcenter.domain.RpcServer;
import com.luixtech.luixrpc.webcenter.exception.DataNotFoundException;
import com.luixtech.luixrpc.webcenter.repository.RpcServerRepository;
import com.luixtech.luixrpc.webcenter.service.RpcConsumerService;
import com.luixtech.luixrpc.webcenter.service.RpcProviderService;
import com.luixtech.luixrpc.webcenter.service.RpcServerService;
import com.luixtech.luixrpc.webcenter.utils.HttpHeaderUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Slf4j
public class RpcServerController {

    @Resource
    private RpcServerRepository rpcServerRepository;
    @Resource
    private RpcServerService    rpcServerService;
    @Resource
    private RpcProviderService  rpcProviderService;
    @Resource
    private RpcConsumerService  rpcConsumerService;

    @ApiOperation("find server by ID in real time")
    @GetMapping("/api/rpc-servers/{id}")
    @Timed
    public ResponseEntity<RpcServer> findById(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        RpcServer domain = rpcServerRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        RpcServer rpcServer = rpcServerService.loadServer(domain.getRegistryIdentity(), domain.getAddress());
        rpcServer.setId(domain.getId());
        rpcServerRepository.save(rpcServer);
        return ResponseEntity.ok(rpcServer);
    }

    @ApiOperation("find server list")
    @GetMapping("/api/rpc-servers")
    @Timed
    public ResponseEntity<List<RpcServer>> findRpcServers(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = ApplicationConstants.DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "address(fuzzy query)") @RequestParam(value = "address", required = false) String address) {
        Page<RpcServer> results = rpcServerService.find(pageable, registryIdentity, address);
        if (!results.isEmpty()) {
            results.getContent().forEach(domain -> {
                if (rpcProviderService.existsAddress(registryIdentity, domain.getAddress(), true)) {
                    domain.setProviding(true);
                } else {
                    domain.setProviding(false);
                }
                if (rpcConsumerService.existsAddress(registryIdentity, domain.getAddress(), true)) {
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
