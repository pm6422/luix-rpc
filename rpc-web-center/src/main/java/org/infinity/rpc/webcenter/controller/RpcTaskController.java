package org.infinity.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.webcenter.component.HttpHeaderCreator;
import org.infinity.rpc.webcenter.domain.RpcScheduledTask;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.RpcScheduledTaskRepository;
import org.infinity.rpc.webcenter.service.RpcScheduledTaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static org.infinity.rpc.webcenter.config.ApplicationConstants.DEFAULT_REG;
import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;


/**
 * REST controller for managing tasks.
 */
@RestController
@Slf4j
public class RpcTaskController {

    @Resource
    private RpcScheduledTaskRepository taskRepository;
    @Resource
    private RpcScheduledTaskService    taskService;
    @Resource
    private HttpHeaderCreator          httpHeaderCreator;

    @ApiOperation("create task")
    @PostMapping("/api/rpc-tasks")
    @Timed
    public ResponseEntity<Void> create(@ApiParam(value = "task", required = true) @Valid @RequestBody RpcScheduledTask domain) {
        log.debug("REST request to create task: {}", domain);
        if (StringUtils.isNotEmpty(domain.getArgumentsJson())) {
            try {
                new ObjectMapper().readValue(domain.getArgumentsJson(), Map.class);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal JSON string format of method arguments");
            }
        }
        taskService.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName())).build();
    }

    @ApiOperation("find task list")
    @GetMapping("/api/rpc-tasks")
    @Timed
    public ResponseEntity<List<RpcScheduledTask>> find(Pageable pageable,
                                                       @ApiParam(value = "registry url identity", required = true, defaultValue = DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
                                                       @ApiParam(value = "Task name(fuzzy query)") @RequestParam(value = "name", required = false) String name,
                                                       @ApiParam(value = "Interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName,
                                                       @ApiParam(value = "Form") @RequestParam(value = "form", required = false) String form,
                                                       @ApiParam(value = "Version") @RequestParam(value = "version", required = false) String version,
                                                       @ApiParam(value = "Method name") @RequestParam(value = "methodName", required = false) String methodName,
                                                       @ApiParam(value = "Method signature") @RequestParam(value = "methodSignature", required = false) String methodSignature) {
        Page<RpcScheduledTask> tasks = taskService.find(pageable, registryIdentity, name, interfaceName, form, version, methodName, methodSignature);
        return ResponseEntity.ok().headers(generatePageHeaders(tasks)).body(tasks.getContent());
    }

    @ApiOperation("find task by id")
    @GetMapping("/api/rpc-tasks/{id}")
    @Timed
    public ResponseEntity<RpcScheduledTask> findById(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        RpcScheduledTask task = taskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(task);
    }

    @ApiOperation("update task")
    @PutMapping("/api/rpc-tasks")
    @Timed
    public ResponseEntity<Void> update(@ApiParam(value = "new task", required = true) @Valid @RequestBody RpcScheduledTask domain) {
        log.debug("REST request to update task: {}", domain);
        if (StringUtils.isNotEmpty(domain.getArgumentsJson())) {
            try {
                new ObjectMapper().readValue(domain.getArgumentsJson(), Map.class);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Illegal JSON string format of method arguments");
            }
        }
        taskService.update(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName())).build();
    }

    @ApiOperation(value = "delete task by id", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/rpc-tasks/{id}")
    @Timed
    public ResponseEntity<Void> delete(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete task: {}", id);
        taskService.delete(id);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", id)).build();
    }
}
