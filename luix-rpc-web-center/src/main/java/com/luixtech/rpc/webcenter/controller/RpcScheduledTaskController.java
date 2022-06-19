package com.luixtech.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luixtech.rpc.webcenter.component.HttpHeaderCreator;
import com.luixtech.rpc.webcenter.config.ApplicationConstants;
import com.luixtech.rpc.webcenter.domain.RpcScheduledTask;
import com.luixtech.rpc.webcenter.exception.DataNotFoundException;
import com.luixtech.rpc.webcenter.repository.RpcScheduledTaskRepository;
import com.luixtech.rpc.webcenter.service.RpcScheduledTaskService;
import com.luixtech.rpc.webcenter.utils.HttpHeaderUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.config.CronTask;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.luixtech.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_FAILFAST;
import static com.luixtech.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE_VAL_FAILOVER;


/**
 * REST controller for managing RPC scheduled tasks.
 */
@RestController
@Slf4j
public class RpcScheduledTaskController {

    @Resource
    private RpcScheduledTaskRepository rpcScheduledTaskRepository;
    @Resource
    private RpcScheduledTaskService    rpcScheduledTaskService;
    @Resource
    private HttpHeaderCreator          httpHeaderCreator;

    @Operation(summary = "create scheduled task")
    @PostMapping("/api/rpc-scheduled-tasks")
    @Timed
    public ResponseEntity<Void> create(@Parameter(description = "task", required = true) @Valid @RequestBody RpcScheduledTask domain) {
        log.debug("REST request to create scheduled task: {}", domain);
        if (domain.getStartTime() != null && domain.getStopTime() != null) {
            Validate.isTrue(domain.getStopTime().isAfter(domain.getStartTime()),
                    "The stop time must be greater than the start time");
        }
        if (StringUtils.isNotEmpty(domain.getArgumentsJson())) {
            try {
                new ObjectMapper().readValue(domain.getArgumentsJson(), Map.class);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal JSON string format of method arguments");
            }
        }
        if (StringUtils.isNotEmpty(domain.getCronExpression())) {
            try {
                new CronTask(null, domain.getCronExpression());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal CRON expression: " + ex.getMessage());
            }
        }

        rpcScheduledTaskService.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName())).build();
    }

    @Operation(summary = "find scheduled task list")
    @GetMapping("/api/rpc-scheduled-tasks")
    @Timed
    public ResponseEntity<List<RpcScheduledTask>> find(Pageable pageable,
                                                       @Parameter(description = "registry url identity", required = true, schema = @Schema(defaultValue = ApplicationConstants.DEFAULT_REG)) @RequestParam(value = "registryIdentity") String registryIdentity,
                                                       @Parameter(description = "Task name(fuzzy query)") @RequestParam(value = "name", required = false) String name,
                                                       @Parameter(description = "Interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName,
                                                       @Parameter(description = "Form") @RequestParam(value = "form", required = false) String form,
                                                       @Parameter(description = "Version") @RequestParam(value = "version", required = false) String version,
                                                       @Parameter(description = "Method name") @RequestParam(value = "methodName", required = false) String methodName,
                                                       @Parameter(description = "Method signature") @RequestParam(value = "methodSignature", required = false) String methodSignature) {
        Page<RpcScheduledTask> tasks = rpcScheduledTaskService.find(pageable, registryIdentity, name, interfaceName, form, version, methodName, methodSignature);
        return ResponseEntity.ok().headers(HttpHeaderUtils.generatePageHeaders(tasks)).body(tasks.getContent());
    }

    @Operation(summary = "find scheduled task by id")
    @GetMapping("/api/rpc-scheduled-tasks/{id}")
    @Timed
    public ResponseEntity<RpcScheduledTask> findById(@Parameter(description = "task ID", required = true) @PathVariable String id) {
        RpcScheduledTask task = rpcScheduledTaskRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        return ResponseEntity.ok(task);
    }

    @Operation(summary = "update scheduled task")
    @PutMapping("/api/rpc-scheduled-tasks")
    @Timed
    public ResponseEntity<Void> update(@Parameter(description = "new task", required = true) @Valid @RequestBody RpcScheduledTask domain) {
        log.debug("REST request to update scheduled task: {}", domain);
        if (domain.getStartTime() != null && domain.getStopTime() != null) {
            Validate.isTrue(domain.getStopTime().isAfter(domain.getStartTime()),
                    "The stop time must be greater than the start time");
        }
        if (StringUtils.isNotEmpty(domain.getArgumentsJson())) {
            try {
                new ObjectMapper().readValue(domain.getArgumentsJson(), Map.class);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal JSON string format of method arguments");
            }
        }
        if (StringUtils.isNotEmpty(domain.getCronExpression())) {
            try {
                new CronTask(null, domain.getCronExpression());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal CRON expression: " + ex.getMessage());
            }
        }
        rpcScheduledTaskService.update(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName())).build();
    }

    @Operation(summary = "delete scheduled task by id", description = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/rpc-scheduled-tasks/{id}")
    @Timed
    public ResponseEntity<Void> delete(@Parameter(description = "task ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete scheduled task: {}", id);
        rpcScheduledTaskService.delete(id);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", id)).build();
    }

    @Operation(summary = "get available time units of fixed rate interval")
    @GetMapping("/api/rpc-scheduled-tasks/fixed-interval-time-units")
    public ResponseEntity<List<String>> getFixedIntervalTimeUnits() {
        return ResponseEntity.ok().body(RpcScheduledTask.AVAILABLE_FIXED_INTERVAL_UNIT);
    }

    @Operation(summary = "get available time units of initial delay")
    @GetMapping("/api/rpc-scheduled-tasks/initial-delay-units")
    public ResponseEntity<List<String>> getInitialDelayUnits() {
        return ResponseEntity.ok().body(RpcScheduledTask.AVAILABLE_INITIAL_DELAY_UNIT);
    }

    @Operation(summary = "get available fault tolerances")
    @GetMapping("/api/rpc-scheduled-tasks/fault-tolerances")
    public ResponseEntity<List<String>> getFaultTolerances() {
        return ResponseEntity.ok().body(Arrays.asList(FAULT_TOLERANCE_VAL_FAILOVER, FAULT_TOLERANCE_VAL_FAILFAST));
    }
}
