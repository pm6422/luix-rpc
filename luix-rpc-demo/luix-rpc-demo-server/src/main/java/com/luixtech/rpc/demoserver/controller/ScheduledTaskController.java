package com.luixtech.rpc.demoserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luixtech.framework.component.HttpHeaderCreator;
import com.luixtech.framework.exception.DataNotFoundException;
import com.luixtech.rpc.demoserver.domain.ScheduledTask;
import com.luixtech.rpc.demoserver.repository.ScheduledTaskRepository;
import com.luixtech.rpc.demoserver.service.ScheduledTaskService;
import com.luixtech.rpc.demoserver.task.schedule.TaskExecutable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.config.CronTask;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.luixtech.framework.utils.HttpHeaderUtils.generatePageHeaders;


/**
 * REST controller for managing scheduled tasks.
 */
@RestController
@AllArgsConstructor
@Slf4j
public class ScheduledTaskController {
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final ScheduledTaskService scheduledTaskService;
    private final HttpHeaderCreator    httpHeaderCreator;
    private final ApplicationContext   applicationContext;

    @Operation(summary = "create scheduled task")
    @PostMapping("/api/scheduled-tasks")
    public ResponseEntity<Void> create(@Parameter(description = "task", required = true) @Valid @RequestBody ScheduledTask domain) {
        log.debug("REST request to create task: {}", domain);
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
        scheduledTaskService.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName())).build();
    }

    @Operation(summary = "find scheduled task list")
    @GetMapping("/api/scheduled-tasks")
    public ResponseEntity<List<ScheduledTask>> find(@ParameterObject Pageable pageable,
                                                    @Parameter(description = "Task name") @RequestParam(value = "name", required = false) String name,
                                                    @Parameter(description = "Bean name") @RequestParam(value = "beanName", required = false) String beanName) {
        Page<ScheduledTask> tasks = scheduledTaskService.find(pageable, name, beanName);
        return ResponseEntity.ok().headers(generatePageHeaders(tasks)).body(tasks.getContent());
    }

    @Operation(summary = "find scheduled task by id")
    @GetMapping("/api/scheduled-tasks/{id}")
    public ResponseEntity<ScheduledTask> findById(@Parameter(description = "task ID", required = true) @PathVariable String id) {
        ScheduledTask scheduledTask = scheduledTaskRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        return ResponseEntity.ok(scheduledTask);
    }

    @Operation(summary = "update scheduled task")
    @PutMapping("/api/scheduled-tasks")
    public ResponseEntity<Void> update(@Parameter(description = "new task", required = true) @Valid @RequestBody ScheduledTask domain) {
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
        scheduledTaskService.update(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName())).build();
    }

    @Operation(summary = "delete scheduled task by id", description = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/scheduled-tasks/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "task ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete scheduled task: {}", id);
        scheduledTaskService.delete(id);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", id)).build();
    }

    @Operation(summary = "find scheduled task bean names")
    @GetMapping("/api/scheduled-tasks/beans")
    public ResponseEntity<List<String>> findBeans() {
        return ResponseEntity.ok().body(Arrays.asList(applicationContext.getBeanNamesForType(TaskExecutable.class)));
    }

    @Operation(summary = "find available time units of fixed rate interval")
    @GetMapping("/api/scheduled-tasks/time-units")
    public ResponseEntity<List<String>> findTimeUnits() {
        return ResponseEntity.ok().body(ScheduledTask.AVAILABLE_FIXED_INTERVAL_UNIT);
    }
}
