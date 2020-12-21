package org.infinity.rpc.appclient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.domain.Authority;
import org.infinity.app.common.service.AuthorityService;
import org.infinity.rpc.appclient.component.HttpHeaderCreator;
import org.infinity.rpc.appclient.exception.NoDataFoundException;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.infinity.rpc.appclient.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing authorities.
 */
@RestController
@Api(tags = "权限管理")
@Slf4j
public class AuthorityController {

    @Consumer
    private       AuthorityService  authorityService;
    private final HttpHeaderCreator httpHeaderCreator;

    public AuthorityController(HttpHeaderCreator httpHeaderCreator) {
        this.httpHeaderCreator = httpHeaderCreator;
    }

    @ApiOperation("分页检索权限列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("/api/authority/authorities")
    public ResponseEntity<List<Authority>> find(Pageable pageable) {
        Page<Authority> authorities = authorityService.findAll(pageable);
        return ResponseEntity.ok().headers(generatePageHeaders(authorities)).body(authorities.getContent());
    }

    @ApiOperation("根据权限名称检索权限")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "权限不存在")})
    @GetMapping("/api/authority/authorities/{name}")
    public ResponseEntity<Authority> findById(
            @ApiParam(value = "权限名称", required = true) @PathVariable String name) {
        Authority authority = authorityService.findById(name).orElseThrow(() -> new NoDataFoundException(name));
        return ResponseEntity.ok(authority);
    }

    @ApiOperation("更新权限")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功更新"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "权限不存在")})
    @PutMapping("/api/authority/authorities")
    public ResponseEntity<Void> update(
            @ApiParam(value = "新的权限", required = true) @Valid @RequestBody Authority domain) {
        log.debug("REST request to update authority: {}", domain);
        authorityService.save(domain);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.updated", domain.getName()))
                .build();
    }

    @ApiOperation(value = "根据名称删除权限", notes = "数据有可能被其他数据所引用，删除之后可能出现一些问题")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功删除"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "权限不存在")})
    @DeleteMapping("/api/authority/authorities/{name}")
    public ResponseEntity<Void> delete(@ApiParam(value = "权限名称", required = true) @PathVariable String name) {
        log.debug("REST request to delete authority: {}", name);
        authorityService.findById(name).orElseThrow(() -> new NoDataFoundException(name));
        authorityService.deleteById(name);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.deleted", name)).build();
    }
}
