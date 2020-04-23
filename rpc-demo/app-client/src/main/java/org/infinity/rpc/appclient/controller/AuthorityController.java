package org.infinity.rpc.appclient.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.infinity.app.common.domain.Authority;
import org.infinity.app.common.dto.AuthorityDTO;
import org.infinity.app.common.service.AuthorityService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * REST controller for managing authorities.
 */
@RestController
@Api(tags = "权限管理")
public class AuthorityController {

    private static final Logger            LOGGER = LoggerFactory.getLogger(AuthorityController.class);
    @Consumer
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
}
