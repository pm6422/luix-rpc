package com.luixtech.rpc.democlient.controller;

import com.luixtech.framework.component.HttpHeaderCreator;
import com.luixtech.rpc.core.client.annotation.RpcConsumer;
import com.luixtech.rpc.democlient.exception.DataNotFoundException;
import com.luixtech.rpc.democommon.domain.Authority;
import com.luixtech.rpc.democommon.service.AuthorityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.luixtech.framework.utils.HttpHeaderUtils.generatePageHeaders;


/**
 * REST controller for managing authorities.
 */
@RestController
@Slf4j
public class AuthorityController {

    @RpcConsumer
    private AuthorityService  authorityService;
    @Resource
    private HttpHeaderCreator httpHeaderCreator;

    @Operation(summary = "create authority")
    @PostMapping("/api/authorities")
    public ResponseEntity<Void> create(
            @Parameter(description = "authority", required = true) @Valid @RequestBody Authority domain) {
        log.debug("REST request to create authority: {}", domain);
        authorityService.save(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName()))
                .build();
    }

    @Operation(summary = "find authority list")
    @GetMapping("/api/authorities")
    public ResponseEntity<List<Authority>> find(@ParameterObject Pageable pageable) {
        Page<Authority> authorities = authorityService.findAll(pageable);
        return ResponseEntity.ok().headers(generatePageHeaders(authorities)).body(authorities.getContent());
    }

    @Operation(summary = "find authority by name")
    @GetMapping("/api/authorities/{name}")
    public ResponseEntity<Authority> findById(
            @Parameter(description = "authority name", required = true) @PathVariable String name) {
        Authority authority = authorityService.findById(name).orElseThrow(() -> new DataNotFoundException(name));
        return ResponseEntity.ok(authority);
    }

    @Operation(summary = "update authority")
    @PutMapping("/api/authorities")
    public ResponseEntity<Void> update(
            @Parameter(description = "new authority", required = true) @Valid @RequestBody Authority domain) {
        log.debug("REST request to update authority: {}", domain);
        authorityService.save(domain);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName()))
                .build();
    }

    @Operation(summary = "delete authority by name", description = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/authorities/{name}")
    public ResponseEntity<Void> delete(@Parameter(description = "authority name", required = true) @PathVariable String name) {
        log.debug("REST request to delete authority: {}", name);
//        authorityService.findById(name).orElseThrow(() -> new DataNotFoundException(name));
        authorityService.deleteById(name);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1003", name)).build();
    }
}
