package org.infinity.rpc.appserver.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.infinity.app.common.domain.Authority;
import org.infinity.app.common.dto.AuthorityDTO;
import org.infinity.rpc.appserver.component.HttpHeaderCreator;
import org.infinity.rpc.appserver.exception.NoDataException;
import org.infinity.rpc.appserver.repository.AuthorityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.*;
import static org.infinity.rpc.appserver.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing authorities.
 */
@RestController
@Api(tags = "权限管理")
@Slf4j
public class AuthorityController {

    private final AuthorityRepository authorityRepository;
    private final HttpHeaderCreator   httpHeaderCreator;

    public AuthorityController(AuthorityRepository authorityRepository, HttpHeaderCreator httpHeaderCreator) {
        this.authorityRepository = authorityRepository;
        this.httpHeaderCreator = httpHeaderCreator;
    }

    @ApiOperation("创建权限")
    @ApiResponses(value = {@ApiResponse(code = SC_CREATED, message = "成功创建")})
    @PostMapping("/api/authority/authorities")
    public ResponseEntity<Void> create(
            @ApiParam(value = "权限", required = true) @Valid @RequestBody AuthorityDTO dto) {
        log.debug("REST request to create authority: {}", dto);
        authorityRepository.insert(Authority.of(dto));
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.created", dto.getName()))
                .build();
    }

    @ApiOperation("(分页)检索权限列表")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功检索")})
    @GetMapping("/api/authority/authorities")
    public ResponseEntity<List<AuthorityDTO>> find(Pageable pageable) throws URISyntaxException {
        Page<Authority> authorities = authorityRepository.findAll(pageable);
        List<AuthorityDTO> DTOs = authorities.getContent().stream().map(Authority::toDTO)
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
        Authority authority = authorityRepository.findById(name).orElseThrow(() -> new NoDataException(name));
        return ResponseEntity.ok(authority.toDTO());
    }

    @ApiOperation("更新权限")
    @ApiResponses(value = {@ApiResponse(code = SC_OK, message = "成功更新"),
            @ApiResponse(code = SC_BAD_REQUEST, message = "权限不存在")})
    @PutMapping("/api/authority/authorities")
    public ResponseEntity<Void> update(
            @ApiParam(value = "新的权限", required = true) @Valid @RequestBody AuthorityDTO dto) {
        log.debug("REST request to update authority: {}", dto);
        authorityRepository.findById(dto.getName()).orElseThrow(() -> new NoDataException(dto.getName()));
        authorityRepository.save(Authority.of(dto));
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
        authorityRepository.findById(name).orElseThrow(() -> new NoDataException(name));
        authorityRepository.deleteById(name);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("notification.authority.deleted", name)).build();
    }
}
