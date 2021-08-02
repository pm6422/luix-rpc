package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.webcenter.domain.RpcConsumer;
import org.infinity.rpc.webcenter.service.RpcConsumerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

import static org.infinity.rpc.webcenter.config.ApplicationConstants.DEFAULT_REG;
import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class RpcConsumerController {

    @Resource
    private RpcConsumerService rpcConsumerService;

    @ApiOperation("find consumer list")
    @GetMapping("/api/rpc-consumer/consumers")
    public ResponseEntity<List<RpcConsumer>> findConsumers(
            Pageable pageable,
            @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG)
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @ApiParam(value = "application name") @RequestParam(value = "application", required = false) String application,
            @ApiParam(value = "address") @RequestParam(value = "address", required = false) String address,
            @ApiParam(value = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @ApiParam(value = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcConsumer> list = rpcConsumerService.find(pageable, registryIdentity, application, address, interfaceName, active);
        return ResponseEntity.ok().headers(generatePageHeaders(list)).body(list.getContent());
    }

    @ApiOperation("get consumer options")
    @GetMapping("/api/rpc-consumer/options")
    public ResponseEntity<Map<String, String>> options() {
        return ResponseEntity.ok(ConsumerStub.OPTIONS);
    }
}
