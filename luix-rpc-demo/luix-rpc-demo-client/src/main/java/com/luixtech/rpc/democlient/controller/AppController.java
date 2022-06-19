package com.luixtech.rpc.democlient.controller;

import com.luixtech.rpc.core.client.annotation.RpcConsumer;
import com.luixtech.rpc.democlient.component.HttpHeaderCreator;
import com.luixtech.rpc.democommon.domain.App;
import com.luixtech.rpc.democommon.service.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.luixtech.rpc.democlient.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing apps.
 */
@RestController
@Slf4j
public class AppController {

    @RpcConsumer(form = "f1", requestTimeout = "10000")
    private AppService        appService;
    @Resource
    private HttpHeaderCreator httpHeaderCreator;

    @Operation(summary = "create application")
    @PostMapping("/api/apps")
    public ResponseEntity<Void> create(@Parameter(description = "application", required = true) @Valid @RequestBody App domain) {
        log.debug("REST request to create app: {}", domain);
        appService.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName())).build();
    }

    @Operation(summary = "find application list")
    @GetMapping("/api/apps")
    public ResponseEntity<List<App>> find(Pageable pageable) {
        Page<App> apps = appService.findAll(pageable);
        return ResponseEntity.ok().headers(generatePageHeaders(apps)).body(apps.getContent());
    }

    @Operation(summary = "find application by name")
    @GetMapping("/api/apps/{name}")
    public ResponseEntity<App> findById(@Parameter(description = "application name", required = true) @PathVariable String name) {
        App app = appService.findById(name);
        return ResponseEntity.ok(app);
    }

    @Operation(summary = "update application")
    @PutMapping("/api/apps")
    public ResponseEntity<Void> update(@Parameter(description = "new application", required = true) @Valid @RequestBody App domain) {
        log.debug("REST request to update app: {}", domain);
        appService.update(domain);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName())).build();
    }

    @Operation(summary = "delete application by name", description = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/apps/{name}")
    public ResponseEntity<Void> delete(@Parameter(description = "application name", required = true) @PathVariable String name) {
        log.debug("REST request to delete app: {}", name);
//        appService.findById(name).orElseThrow(() -> new DataNotFoundException(name));
        appService.deleteById(name);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1003", name)).build();
    }
}
