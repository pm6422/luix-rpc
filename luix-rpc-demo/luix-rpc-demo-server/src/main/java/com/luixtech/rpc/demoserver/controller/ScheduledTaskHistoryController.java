package com.luixtech.rpc.demoserver.controller;

import com.luixtech.rpc.demoserver.domain.ScheduledTaskHistory;
import com.luixtech.rpc.demoserver.repository.ScheduledTaskHistoryRepository;
import com.luixtech.utilities.exception.DataNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
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

import java.util.List;

import static com.luixtech.springbootframework.utils.HttpHeaderUtils.generatePageHeaders;


/**
 * REST controller for managing scheduled task histories.
 */
@RestController
@AllArgsConstructor
@Slf4j
public class ScheduledTaskHistoryController {
    private final ScheduledTaskHistoryRepository scheduledTaskHistoryRepository;

    @Operation(summary = "find task history list")
    @GetMapping("/api/scheduled-task-histories")
    public ResponseEntity<List<ScheduledTaskHistory>> find(@ParameterObject Pageable pageable,
                                                           @Parameter(description = "Task name") @RequestParam(value = "name", required = false) String name) {
        ScheduledTaskHistory probe = new ScheduledTaskHistory();
        probe.setName(name);
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Page<ScheduledTaskHistory> histories = scheduledTaskHistoryRepository.findAll(Example.of(probe, matcher), pageable);
        return ResponseEntity.ok().headers(generatePageHeaders(histories)).body(histories.getContent());
    }

    @Operation(summary = "find task history by id")
    @GetMapping("/api/scheduled-task-histories/{id}")
    public ResponseEntity<ScheduledTaskHistory> findById(@Parameter(description = "task ID", required = true) @PathVariable String id) {
        ScheduledTaskHistory history = scheduledTaskHistoryRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        return ResponseEntity.ok(history);
    }
}
