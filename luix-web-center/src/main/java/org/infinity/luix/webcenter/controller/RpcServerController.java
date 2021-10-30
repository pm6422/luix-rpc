package org.infinity.luix.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.webcenter.config.ApplicationConstants;
import org.infinity.luix.webcenter.domain.RpcServer;
import org.infinity.luix.webcenter.service.RpcConsumerService;
import org.infinity.luix.webcenter.service.RpcProviderService;
import org.infinity.luix.webcenter.service.RpcServerService;
import org.infinity.luix.webcenter.utils.HttpHeaderUtils;
import org.infinity.luix.webcenter.exception.DataNotFoundException;
import org.infinity.luix.webcenter.repository.RpcServerRepository;
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
                    domain.setActive(true);
                }
                if (rpcConsumerService.existsAddress(registryIdentity, domain.getAddress(), true)) {
                    domain.setConsuming(true);
                    domain.setActive(true);
                }
            });
        }
        return ResponseEntity.ok().headers(HttpHeaderUtils.generatePageHeaders(results)).body(results.getContent());
    }
}
