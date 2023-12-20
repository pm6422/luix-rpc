package com.luixtech.rpc.webcenter.controller;

import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.webcenter.domain.RpcConsumer;
import com.luixtech.rpc.webcenter.service.RpcConsumerService;
import com.luixtech.rpc.webcenter.utils.HttpHeaderUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.luixtech.rpc.webcenter.LuixRpcWebCenterApplication.DEFAULT_REG;


@RestController
@AllArgsConstructor
@Slf4j
public class RpcConsumerController {
    private final RpcConsumerService rpcConsumerService;

    @Operation(summary = "find consumer list")
    @GetMapping("/api/rpc-consumers")
    public ResponseEntity<List<RpcConsumer>> findConsumers(
            @ParameterObject Pageable pageable,
            @Parameter(description = "registry url identity", required = true, schema = @Schema(defaultValue = DEFAULT_REG))
            @RequestParam(value = "registryIdentity") String registryIdentity,
            @Parameter(description = "application name") @RequestParam(value = "application", required = false) String application,
            @Parameter(description = "address") @RequestParam(value = "address", required = false) String address,
            @Parameter(description = "interface name(fuzzy query)") @RequestParam(value = "interfaceName", required = false) String interfaceName,
            @Parameter(description = "active flag") @RequestParam(value = "active", required = false) Boolean active) {
        Page<RpcConsumer> list = rpcConsumerService.find(pageable, registryIdentity, application, address, interfaceName, active);
        return ResponseEntity.ok().headers(HttpHeaderUtils.generatePageHeaders(list)).body(list.getContent());
    }

    @Operation(summary = "get consumer options")
    @GetMapping("/api/rpc-consumers/options")
    public ResponseEntity<Map<String, String>> options() {
        return ResponseEntity.ok(ConsumerStub.OPTIONS);
    }
}
