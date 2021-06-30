package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcServiceRepository;
import org.infinity.rpc.webcenter.service.RpcConsumerService;
import org.infinity.rpc.webcenter.service.RpcProviderService;
import org.infinity.rpc.webcenter.service.RpcServiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class RpcServiceController {

    @Resource
    private RpcServiceService    rpcServiceService;
    @Resource
    private RpcServiceRepository rpcServiceRepository;
    @Resource
    private RpcProviderService   rpcProviderService;
    @Resource
    private RpcConsumerService   rpcConsumerService;

    @ApiOperation("find service")
    @GetMapping("/api/rpc-service")
    public ResponseEntity<RpcService> findById(
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry")
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        String id = DigestUtils.md5DigestAsHex((interfaceName + "@" + registryIdentity).getBytes());
        RpcService domain = rpcServiceRepository.findById(id).orElseThrow(() -> new NoDataFoundException(interfaceName));
        if (rpcProviderService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
            domain.setProviding(true);
        }
        if (rpcConsumerService.existsService(registryIdentity, domain.getInterfaceName(), true)) {
            domain.setConsuming(true);
        }
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("find service list")
    @GetMapping("/api/rpc-service/services")
    public ResponseEntity<List<RpcService>> findRpcServices(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry")
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName) {
        Page<RpcService> results = rpcServiceService.find(pageable, registryIdentity, interfaceName);
        if (!results.isEmpty()) {
            results.getContent().forEach(service -> {
                if (rpcProviderService.existsService(registryIdentity, service.getInterfaceName(), true)) {
                    service.setProviding(true);
                }
                if (rpcConsumerService.existsService(registryIdentity, service.getInterfaceName(), true)) {
                    service.setConsuming(true);
                }
            });
        }
        return ResponseEntity.ok().headers(generatePageHeaders(results)).body(results.getContent());
    }
}
