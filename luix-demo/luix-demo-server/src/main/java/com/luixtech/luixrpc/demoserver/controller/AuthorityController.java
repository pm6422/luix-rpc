package com.luixtech.luixrpc.demoserver.controller;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.democommon.domain.Authority;
import com.luixtech.luixrpc.demoserver.component.HttpHeaderCreator;
import com.luixtech.luixrpc.demoserver.exception.DuplicationException;
import com.luixtech.luixrpc.demoserver.exception.DataNotFoundException;
import com.luixtech.luixrpc.demoserver.repository.AuthorityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.luixtech.luixrpc.demoserver.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing authorities.
 */
@RestController
@Slf4j
public class AuthorityController {

    @Resource
    private AuthorityRepository authorityRepository;
    @Resource
    private HttpHeaderCreator   httpHeaderCreator;

    @ApiOperation("create authority")
    @PostMapping("/api/authorities")
    public ResponseEntity<Void> create(@ApiParam(value = "authority", required = true) @Valid @RequestBody Authority domain) {
        log.debug("REST request to create authority: {}", domain);
        authorityRepository.findById(domain.getName()).ifPresent(app -> {
            throw new DuplicationException(ImmutableMap.of("name", domain.getName()));
        });
        authorityRepository.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName()))
                .build();
    }

    @ApiOperation("find authority list")
    @GetMapping("/api/authorities")
    public ResponseEntity<List<Authority>> find(Pageable pageable) {
        Page<Authority> authorities = authorityRepository.findAll(pageable);
        HttpHeaders headers = generatePageHeaders(authorities);
        return ResponseEntity.ok().headers(headers).body(authorities.getContent());
    }

    @ApiOperation("find authority by name")
    @GetMapping("/api/authorities/{name}")
    public ResponseEntity<Authority> findById(
            @ApiParam(value = "authority name", required = true) @PathVariable String name) {
        Authority domain = authorityRepository.findById(name).orElseThrow(() -> new DataNotFoundException(name));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("update authority")
    @PutMapping("/api/authorities")
    public ResponseEntity<Void> update(
            @ApiParam(value = "new authority", required = true) @Valid @RequestBody Authority domain) {
        log.debug("REST request to update authority: {}", domain);
        authorityRepository.findById(domain.getName()).orElseThrow(() -> new DataNotFoundException(domain.getName()));
        authorityRepository.save(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName())).build();
    }

    @ApiOperation(value = "delete authority by name", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/authorities/{name}")
    public ResponseEntity<Void> delete(@ApiParam(value = "authority name", required = true) @PathVariable String name) {
        log.debug("REST request to delete authority: {}", name);
        if (!authorityRepository.existsById(name)) {
            throw new DataNotFoundException(name);
        }
        authorityRepository.deleteById(name);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", name)).build();
    }
}
