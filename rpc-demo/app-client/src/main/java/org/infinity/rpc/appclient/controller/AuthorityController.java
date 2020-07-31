package org.infinity.rpc.appclient.controller;

import io.swagger.annotations.*;
import org.infinity.app.common.domain.Authority;
import org.infinity.app.common.dto.AuthorityDTO;
import org.infinity.app.common.service.AuthorityService;
import org.infinity.rpc.appclient.exception.NoDataException;
import org.infinity.rpc.appclient.utils.HttpHeaderCreator;
import org.infinity.rpc.appclient.utils.PaginationUtils;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * REST controller for managing authorities.
 */
@RestController
@Api(tags = "权限管理")
public class AuthorityController {

    private static final Logger            LOGGER = LoggerFactory.getLogger(AuthorityController.class);
    @Consumer(timeout = 1000)
    private              AuthorityService  authorityService;
    @Autowired
    private              HttpHeaderCreator httpHeaderCreator;

    @ApiOperation("获取权限列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("/api/authority/authorities")
    public ResponseEntity<List<AuthorityDTO>> find(Pageable pageable) throws URISyntaxException {
        Page<Authority> authorities = authorityService.findAll(pageable);
        List<AuthorityDTO> DTOs = authorities.getContent().stream().map(auth -> auth.asDTO())
                .collect(Collectors.toList());
        HttpHeaders headers = PaginationUtils.generatePaginationHttpHeaders(authorities, "/api/authority/authorities");
        return ResponseEntity.ok().headers(headers).body(DTOs);
    }

    @ApiOperation("获取所有权限")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取")})
    @GetMapping("/api/authority/authorities/all")
    public ResponseEntity<List<AuthorityDTO>> findAll() {
        List<AuthorityDTO> authDTOs = authorityService.findAll().stream().map(entity -> entity.asDTO())
                .collect(Collectors.toList());
        return ResponseEntity.ok(authDTOs);
    }

    @ApiOperation("根据权限名称检索权限信息")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功获取"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "权限信息不存在")})
    @GetMapping("/api/authority/authorities/{name}")
    public ResponseEntity<AuthorityDTO> findById(
            @ApiParam(value = "权限名称", required = true) @PathVariable String name) {
        Authority authority = authorityService.findById(name).orElseThrow(() -> new NoDataException(name));
        return ResponseEntity.ok(authority.asDTO());
    }

    @ApiOperation("更新权限信息")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功更新"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "权限信息不存在")})
    @PutMapping("/api/authority/authorities")
    public ResponseEntity<Void> update(
            @ApiParam(value = "新的权限信息", required = true) @Valid @RequestBody AuthorityDTO dto) {
        LOGGER.debug("REST request to update authority: {}", dto);
        authorityService.findById(dto.getName()).orElseThrow(() -> new NoDataException(dto.getName()));
        authorityService.save(Authority.of(dto));
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.updated", dto.getName()))
                .build();
    }

    @ApiOperation(value = "根据权限名称删除权限信息", notes = "数据有可能被其他数据所引用，删除之后可能出现一些问题")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功删除"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "权限信息不存在")})
    @DeleteMapping("/api/authority/authorities/{name}")
    public ResponseEntity<Void> delete(@ApiParam(value = "权限名称", required = true) @PathVariable String name) {
        LOGGER.debug("REST request to delete authority: {}", name);
        authorityService.findById(name).orElseThrow(() -> new NoDataException(name));
        authorityService.deleteById(name);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.deleted", name)).build();
    }
}
