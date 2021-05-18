package org.infinity.rpc.democlient.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.democlient.component.HttpHeaderCreator;
import org.infinity.rpc.democlient.exception.NoDataFoundException;
import org.infinity.rpc.democommon.domain.Authority;
import org.infinity.rpc.democommon.service.AuthorityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static org.infinity.rpc.democlient.utils.HttpHeaderUtils.generatePageHeaders;
import static org.infinity.rpc.democommon.domain.Authority.ADMIN;
import static org.infinity.rpc.democommon.domain.Authority.USER;

/**
 * REST controller for managing authorities.
 */
@RestController
@Slf4j
public class AuthorityController {

    @Consumer
    private AuthorityService  authorityService;
    @Resource
    private HttpHeaderCreator httpHeaderCreator;

    @ApiOperation("find authority list")
    @GetMapping("/api/authorities")
    public ResponseEntity<List<Authority>> find(Pageable pageable) {
        Page<Authority> authorities = authorityService.findAll(pageable);
        return ResponseEntity.ok().headers(generatePageHeaders(authorities)).body(authorities.getContent());
    }

    @ApiOperation("find authority by name")
    @GetMapping("/api/authorities/{name}")
    public ResponseEntity<Authority> findById(
            @ApiParam(value = "authority name", required = true) @PathVariable String name) {
        Authority authority = authorityService.findById(name).orElseThrow(() -> new NoDataFoundException(name));
        return ResponseEntity.ok(authority);
    }

    @ApiOperation("find authority names")
    @GetMapping("/api/authority-names")
    public ResponseEntity<List<String>> find() {
        Query query = Query.query(Criteria.where("name").in(ADMIN, USER));
        List<String> authorities = authorityService.find(query).stream().map(Authority::getName).collect(Collectors.toList());
        return ResponseEntity.ok(authorities);
    }

    @ApiOperation("update authority")
    @PutMapping("/api/authorities")
    public ResponseEntity<Void> update(
            @ApiParam(value = "new authority", required = true) @Valid @RequestBody Authority domain) {
        log.debug("REST request to update authority: {}", domain);
        authorityService.save(domain);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName()))
                .build();
    }

    @ApiOperation(value = "delete authority by name", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/authorities/{name}")
    public ResponseEntity<Void> delete(@ApiParam(value = "authority name", required = true) @PathVariable String name) {
        log.debug("REST request to delete authority: {}", name);
        authorityService.findById(name).orElseThrow(() -> new NoDataFoundException(name));
        authorityService.deleteById(name);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1003", name)).build();
    }
}
