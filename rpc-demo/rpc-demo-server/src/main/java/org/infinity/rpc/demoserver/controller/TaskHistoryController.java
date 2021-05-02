package org.infinity.rpc.demoserver.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.demoserver.domain.TaskHistory;
import org.infinity.rpc.demoserver.exception.NoDataFoundException;
import org.infinity.rpc.demoserver.repository.TaskHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static org.infinity.rpc.demoserver.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing task histories.
 */
@RestController
@Slf4j
public class TaskHistoryController {

    @Resource
    private TaskHistoryRepository taskHistoryRepository;

    @ApiOperation("find task history list")
    @GetMapping("/api/task-history/histories")
    public ResponseEntity<List<TaskHistory>> find(Pageable pageable) {
        Page<TaskHistory> histories = taskHistoryRepository.findAll(pageable);
        return ResponseEntity.ok().headers(generatePageHeaders(histories)).body(histories.getContent());
    }

    @ApiOperation("find task history by id")
    @GetMapping("/api/task-history/histories/{id}")
    public ResponseEntity<TaskHistory> findById(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        TaskHistory history = taskHistoryRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(history);
    }
}
