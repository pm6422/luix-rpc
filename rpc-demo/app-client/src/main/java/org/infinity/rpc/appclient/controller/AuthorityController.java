package org.infinity.rpc.appclient.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.domain.Authority;
import org.infinity.app.common.dto.AuthorityDTO;
import org.infinity.app.common.service.AuthorityService;
import org.infinity.rpc.appclient.component.HttpHeaderCreator;
import org.infinity.rpc.appclient.exception.NoDataException;
import org.infinity.rpc.core.client.annotation.Consumer;
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

    @ApiOperation("(分页)检索权限列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("/api/authority/authorities")
    public ResponseEntity<List<AuthorityDTO>> find(Pageable pageable) throws URISyntaxException {
        Page<Authority> authorities = authorityService.findAll(pageable);
        List<AuthorityDTO> DTOs = authorities.getContent().stream().map(Authority::asDTO)
                .collect(Collectors.toList());
        HttpHeaders headers = generatePageHeaders(authorities, "/api/authority/authorities");
        return ResponseEntity.ok().headers(headers).body(DTOs);
    }

    @ApiOperation("根据权限名称检索权限")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "权限不存在")})
    @GetMapping("/api/authority/authorities/{name}")
    public ResponseEntity<AuthorityDTO> findById(
            @ApiParam(value = "权限名称", required = true) @PathVariable String name) {
        Authority authority = authorityService.findById(name).orElseThrow(() -> new NoDataException(name));
        return ResponseEntity.ok(authority.asDTO());
    }

    @ApiOperation("更新权限")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功更新"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "权限不存在")})
    @PutMapping("/api/authority/authorities")
    public ResponseEntity<Void> update(
            @ApiParam(value = "新的权限", required = true) @Valid @RequestBody AuthorityDTO dto) {
        log.debug("REST request to update authority: {}", dto);
        authorityService.findById(dto.getName()).orElseThrow(() -> new NoDataException(dto.getName()));
        authorityService.save(Authority.of(dto));
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.updated", dto.getName()))
                .build();
    }

    @ApiOperation(value = "根据名称删除权限", notes = "数据有可能被其他数据所引用，删除之后可能出现一些问题")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功删除"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "权限不存在")})
    @DeleteMapping("/api/authority/authorities/{name}")
    public ResponseEntity<Void> delete(@ApiParam(value = "权限名称", required = true) @PathVariable String name) {
        log.debug("REST request to delete authority: {}", name);
        authorityService.findById(name).orElseThrow(() -> new NoDataException(name));
        authorityService.deleteById(name);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.deleted", name)).build();
    }
}
