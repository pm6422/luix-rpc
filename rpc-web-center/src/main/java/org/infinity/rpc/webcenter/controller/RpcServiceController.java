package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.domain.RpcService;
import org.infinity.rpc.webcenter.service.RpcServiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
    private RpcServiceService rpcServiceService;

    @ApiOperation("find service list")
    @GetMapping("/api/rpc-service/services")
    public ResponseEntity<List<RpcService>> findRpcServices(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = "zookeeper://localhost:2181/registry")
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @ApiParam(value = "providing flag") @RequestParam(value = "providing", required = false) Boolean providing,
            @ApiParam(value = "consuming flag") @RequestParam(value = "consuming", required = false) Boolean consuming) {
        Page<RpcService> list = rpcServiceService.find(pageable, registryIdentity, interfaceName, providing, consuming);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }
}
