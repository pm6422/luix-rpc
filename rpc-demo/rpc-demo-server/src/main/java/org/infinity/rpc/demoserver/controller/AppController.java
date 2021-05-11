package org.infinity.rpc.demoserver.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.democommon.domain.App;
import org.infinity.rpc.democommon.service.AppService;
import org.infinity.rpc.demoserver.component.HttpHeaderCreator;
import org.infinity.rpc.demoserver.exception.NoDataFoundException;
import org.infinity.rpc.demoserver.repository.AppRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static org.infinity.rpc.demoserver.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing apps.
 * <p>
 * REST API standard
 * https://www.toutiao.com/i6958494366755226119/
 * https://www.toutiao.com/i6808027725568147982/
 * https://www.toutiao.com/i6915313349622465027/
 */
@RestController
@Slf4j
public class AppController {

    @Resource
    private AppRepository     appRepository;
    @Resource(name = "appService1Impl")
    private AppService        appService;
    @Resource
    private HttpHeaderCreator httpHeaderCreator;

    @ApiOperation("create application")
    @PostMapping("/api/apps")
    public ResponseEntity<Void> create(@ApiParam(value = "application", required = true) @Valid @RequestBody App domain) {
        log.debug("REST request to create app: {}", domain);
        appService.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName())).build();
    }

    @ApiOperation("find application list")
    @GetMapping("/api/apps")
    public ResponseEntity<List<App>> find(Pageable pageable) {
        Page<App> apps = appRepository.findAll(pageable);
        return ResponseEntity.ok().headers(generatePageHeaders(apps)).body(apps.getContent());
    }

    @ApiOperation("find application by name")
    @GetMapping("/api/apps/{name}")
    public ResponseEntity<App> findById(@ApiParam(value = "application name", required = true) @PathVariable String name) {
        App app = appRepository.findById(name).orElseThrow(() -> new NoDataFoundException(name));
        return ResponseEntity.ok(app);
    }

    @ApiOperation("update application")
    @PutMapping("/api/apps")
    public ResponseEntity<Void> update(@ApiParam(value = "new application", required = true) @Valid @RequestBody App domain) {
        log.debug("REST request to update app: {}", domain);
        appService.update(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName())).build();
    }

    @ApiOperation(value = "delete application by name", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/apps/{name}")
    public ResponseEntity<Void> delete(@ApiParam(value = "application name", required = true) @PathVariable String name) {
        log.debug("REST request to delete app: {}", name);
        if (!appRepository.existsById(name)) {
            throw new NoDataFoundException(name);
        }
        appRepository.deleteById(name);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", name)).build();
    }
}
