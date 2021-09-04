package org.infinity.luix.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.webcenter.component.HttpHeaderCreator;
import org.infinity.luix.webcenter.domain.App;
import org.infinity.luix.webcenter.repository.AppAuthorityRepository;
import org.infinity.luix.webcenter.repository.AppRepository;
import org.infinity.luix.webcenter.service.AppService;
import org.infinity.luix.webcenter.utils.HttpHeaderUtils;
import org.infinity.luix.webcenter.domain.AppAuthority;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.exception.NoDataFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for managing apps.
 */
@RestController
@Slf4j
public class AppController {

    @Resource
    private AppRepository          appRepository;
    @Resource
    private AppAuthorityRepository appAuthorityRepository;
    @Resource
    private AppService             appService;
    @Resource
    private HttpHeaderCreator      httpHeaderCreator;

    @ApiOperation("create application")
    @PostMapping("/api/apps")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> create(@ApiParam(value = "application", required = true) @Valid @RequestBody App domain) {
        log.debug("REST request to create app: {}", domain);
        appService.insert(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getName())).build();
    }

    @ApiOperation("find application list")
    @GetMapping("/api/apps")
    @Secured({Authority.ADMIN})
    public ResponseEntity<List<App>> find(Pageable pageable) {
        Page<App> apps = appRepository.findAll(pageable);
        return ResponseEntity.ok().headers(HttpHeaderUtils.generatePageHeaders(apps)).body(apps.getContent());
    }

    @ApiOperation("find application by name")
    @GetMapping("/api/apps/{name}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<App> findById(@ApiParam(value = "name", required = true) @PathVariable String name) {
        App app = appRepository.findById(name).orElseThrow(() -> new NoDataFoundException(name));
        List<AppAuthority> appAuthorities = appAuthorityRepository.findByAppName(name);
        Set<String> authorities = appAuthorities.stream().map(AppAuthority::getAuthorityName).collect(Collectors.toSet());
        app.setAuthorities(authorities);
        return ResponseEntity.ok(app);
    }

    @ApiOperation("update application")
    @PutMapping("/api/apps")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> update(@ApiParam(value = "new application", required = true) @Valid @RequestBody App domain) {
        log.debug("REST request to update app: {}", domain);
        appService.update(domain);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getName())).build();
    }

    @ApiOperation(value = "delete application by name", notes = "the data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/apps/{name}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<Void> delete(@ApiParam(value = "name", required = true) @PathVariable String name) {
        log.debug("REST request to delete app: {}", name);
        appRepository.findById(name).orElseThrow(() -> new NoDataFoundException(name));
        appRepository.deleteById(name);
        appAuthorityRepository.deleteByAppName(name);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", name)).build();
    }
}
