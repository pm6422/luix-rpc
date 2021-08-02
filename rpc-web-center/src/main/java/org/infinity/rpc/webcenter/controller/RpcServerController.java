package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.domain.RpcServer;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcServerRepository;
import org.infinity.rpc.webcenter.service.RpcConsumerService;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.infinity.rpc.webcenter.service.RpcServerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static org.infinity.rpc.webcenter.config.ApplicationConstants.DEFAULT_REG;
import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

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
    @GetMapping("/api/rpc-server/{id}")
    public ResponseEntity<RpcServer> findById(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        RpcServer domain = rpcServerRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        RpcServer rpcServer = rpcServerService.loadServer(domain.getRegistryIdentity(), domain.getAddress());
        rpcServer.setId(domain.getId());
        rpcServerRepository.save(rpcServer);
        return ResponseEntity.ok(rpcServer);
    }

    @ApiOperation("find server list")
    @GetMapping("/api/rpc-server/servers")
    public ResponseEntity<List<RpcServer>> findRpcServers(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "address(fuzzy query)") @RequestParam(value = "address", required = false) String address) {
        Page<RpcServer> results = rpcServerService.find(pageable, registryIdentity, address);
        if (!results.isEmpty()) {
            results.getContent().forEach(domain -> {
                if (rpcProviderService.existsAddress(registryIdentity, domain.getAddress(), true)) {
                    domain.setProviding(true);
                }
                if (rpcConsumerService.existsAddress(registryIdentity, domain.getAddress(), true)) {
                    domain.setConsuming(true);
                }
                if (domain.isProviding() || domain.isConsuming()) {
                    domain.setActive(true);
                } else {
                    domain.setActive(false);
                }
            });
        }
        return ResponseEntity.ok().headers(generatePageHeaders(results)).body(results.getContent());
    }
}
