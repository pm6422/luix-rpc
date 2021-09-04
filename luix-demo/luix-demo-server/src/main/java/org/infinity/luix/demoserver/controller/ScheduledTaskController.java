package org.infinity.luix.demoserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.luix.demoserver.component.HttpHeaderCreator;
import org.infinity.luix.demoserver.domain.ScheduledTask;
import org.infinity.luix.demoserver.service.ScheduledTaskService;
import org.infinity.luix.demoserver.exception.NoDataFoundException;
import org.infinity.luix.demoserver.repository.ScheduledTaskRepository;
import org.infinity.luix.demoserver.task.TaskExecutable;
import org.springframework.context.ApplicationContext;
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

import static org.infinity.luix.demoserver.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing scheduled tasks.
 */
@RestController
@Slf4j
public class ScheduledTaskController {

    @Resource
    private ScheduledTaskRepository scheduledTaskRepository;
    @Resource
    private ScheduledTaskService    scheduledTaskService;
    @Resource
    private HttpHeaderCreator       httpHeaderCreator;
    @Resource
    private ApplicationContext      applicationContext;

    @ApiOperation("create scheduled task")
    @PostMapping("/api/scheduled-tasks")
    public ResponseEntity<Void> create(@ApiParam(value = "task", required = true) @Valid @RequestBody ScheduledTask domain) {
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

    @ApiOperation("find scheduled task list")
    @GetMapping("/api/scheduled-tasks")
    public ResponseEntity<List<ScheduledTask>> find(Pageable pageable,
                                                    @ApiParam(value = "Task name") @RequestParam(value = "name", required = false) String name,
                                                    @ApiParam(value = "Bean name") @RequestParam(value = "beanName", required = false) String beanName) {
        Page<ScheduledTask> tasks = scheduledTaskService.find(pageable, name, beanName);
        return ResponseEntity.ok().headers(generatePageHeaders(tasks)).body(tasks.getContent());
    }

    @ApiOperation("find scheduled task by id")
    @GetMapping("/api/scheduled-tasks/{id}")
    public ResponseEntity<ScheduledTask> findById(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        ScheduledTask scheduledTask = scheduledTaskRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(scheduledTask);
    }

    @ApiOperation("update scheduled task")
    @PutMapping("/api/scheduled-tasks")
    public ResponseEntity<Void> update(@ApiParam(value = "new task", required = true) @Valid @RequestBody ScheduledTask domain) {
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

    @ApiOperation(value = "delete scheduled task by id", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/scheduled-tasks/{id}")
    public ResponseEntity<Void> delete(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete scheduled task: {}", id);
        scheduledTaskService.delete(id);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", id)).build();
    }

    @ApiOperation("find scheduled task bean names")
    @GetMapping("/api/scheduled-tasks/beans")
    public ResponseEntity<List<String>> findBeans() {
        return ResponseEntity.ok().body(Arrays.asList(applicationContext.getBeanNamesForType(TaskExecutable.class)));
    }

    @ApiOperation("find available time units of fixed rate interval")
    @GetMapping("/api/scheduled-tasks/time-units")
    public ResponseEntity<List<String>> findTimeUnits() {
        return ResponseEntity.ok().body(ScheduledTask.AVAILABLE_FIXED_INTERVAL_UNIT);
    }
}
