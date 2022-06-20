package com.luixtech.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import com.luixtech.rpc.webcenter.config.ApplicationConstants;
import com.luixtech.rpc.webcenter.domain.RpcScheduledTaskHistory;
import com.luixtech.rpc.webcenter.exception.DataNotFoundException;
import com.luixtech.rpc.webcenter.repository.RpcScheduledTaskHistoryRepository;
import com.luixtech.rpc.webcenter.utils.HttpHeaderUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * REST controller for managing RPC scheduled task histories.
 */
@RestController
@Slf4j
public class RpcScheduledTaskHistoryController {

    @Resource
    private RpcScheduledTaskHistoryRepository rpcScheduledTaskHistoryRepository;

    @Operation(summary = "find scheduled task history list")
    @GetMapping("/api/rpc-scheduled-task-histories")
    @Timed
    public ResponseEntity<List<RpcScheduledTaskHistory>> find(@ParameterObject Pageable pageable,
                                                              @Parameter(description = "registry url identity", required = true, schema = @Schema(defaultValue = ApplicationConstants.DEFAULT_REG)) @RequestParam(value = "registryIdentity") String registryIdentity,
                                                              @Parameter(description = "Task name") @RequestParam(value = "name", required = false) String name,
                                                              @Parameter(description = "Interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName,
                                                              @Parameter(description = "Form") @RequestParam(value = "form", required = false) String form,
                                                              @Parameter(description = "Version") @RequestParam(value = "version", required = false) String version,
                                                              @Parameter(description = "Method signature") @RequestParam(value = "methodSignature", required = false) String methodSignature) {
        RpcScheduledTaskHistory probe = new RpcScheduledTaskHistory();
        probe.setRegistryIdentity(registryIdentity);
        probe.setName(trimToNull(name));
        probe.setInterfaceName(trimToNull(interfaceName));
        probe.setForm(trimToNull(form));
        probe.setVersion(trimToNull(version));
        probe.setMethodSignature(trimToNull(methodSignature));
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Page<RpcScheduledTaskHistory> histories = rpcScheduledTaskHistoryRepository.findAll(Example.of(probe, matcher), pageable);
        return ResponseEntity.ok().headers(HttpHeaderUtils.generatePageHeaders(histories)).body(histories.getContent());
    }

    @Operation(summary = "find scheduled task history by ID")
    @GetMapping("/api/rpc-scheduled-task-histories/{id}")
    @Timed
    public ResponseEntity<RpcScheduledTaskHistory> findById(@Parameter(description = "task ID", required = true) @PathVariable String id) {
        RpcScheduledTaskHistory history = rpcScheduledTaskHistoryRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        return ResponseEntity.ok(history);
    }
}
