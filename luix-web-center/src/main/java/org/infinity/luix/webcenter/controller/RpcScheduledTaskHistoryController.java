package org.infinity.luix.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.webcenter.config.ApplicationConstants;
import org.infinity.luix.webcenter.utils.HttpHeaderUtils;
import org.infinity.luix.webcenter.domain.RpcScheduledTaskHistory;
import org.infinity.luix.webcenter.exception.DataNotFoundException;
import org.infinity.luix.webcenter.repository.RpcScheduledTaskHistoryRepository;
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

    @ApiOperation("find scheduled task history list")
    @GetMapping("/api/rpc-scheduled-task-histories")
    @Timed
    public ResponseEntity<List<RpcScheduledTaskHistory>> find(Pageable pageable,
                                                              @ApiParam(value = "registry url identity", required = true, defaultValue = ApplicationConstants.DEFAULT_REG) @RequestParam(value = "registryIdentity") String registryIdentity,
                                                              @ApiParam(value = "Task name") @RequestParam(value = "name", required = false) String name,
                                                              @ApiParam(value = "Interface name") @RequestParam(value = "interfaceName", required = false) String interfaceName,
                                                              @ApiParam(value = "Form") @RequestParam(value = "form", required = false) String form,
                                                              @ApiParam(value = "Version") @RequestParam(value = "version", required = false) String version,
                                                              @ApiParam(value = "Method signature") @RequestParam(value = "methodSignature", required = false) String methodSignature) {
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

    @ApiOperation("find scheduled task history by id")
    @GetMapping("/api/rpc-scheduled-task-histories/{id}")
    @Timed
    public ResponseEntity<RpcScheduledTaskHistory> findById(@ApiParam(value = "task ID", required = true) @PathVariable String id) {
        RpcScheduledTaskHistory history = rpcScheduledTaskHistoryRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        return ResponseEntity.ok(history);
    }
}
