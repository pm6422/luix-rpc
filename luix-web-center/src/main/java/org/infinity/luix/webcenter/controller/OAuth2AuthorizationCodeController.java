package org.infinity.luix.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.webcenter.component.HttpHeaderCreator;
import org.infinity.luix.webcenter.domain.MongoOAuth2AuthorizationCode;
import org.infinity.luix.webcenter.repository.OAuth2AuthorizationCodeRepository;
import org.infinity.luix.webcenter.utils.HttpHeaderUtils;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.exception.NoDataFoundException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@Slf4j
public class OAuth2AuthorizationCodeController {

    @Resource
    private OAuth2AuthorizationCodeRepository oAuth2AuthorizationCodeRepository;
    @Resource
    private HttpHeaderCreator                 httpHeaderCreator;

    /**
     * Authorization code will be deleted immediately after authentication process.
     * So the database will be always empty.
     *
     * @param pageable            pagination info
     * @param authorizationCodeId authorization code id
     * @param code                authorization code
     * @return code list
     */
    @ApiOperation("find authorization code list")
    @GetMapping("/api/oauth2-authorization-codes")
    @Secured(Authority.ADMIN)
    public ResponseEntity<List<MongoOAuth2AuthorizationCode>> find(Pageable pageable,
                                                                   @ApiParam(value = "ID") @RequestParam(value = "authorizationCodeId", required = false) String authorizationCodeId,
                                                                   @ApiParam(value = "authorization code") @RequestParam(value = "code", required = false) String code) {
        MongoOAuth2AuthorizationCode probe = new MongoOAuth2AuthorizationCode();
        probe.setId(authorizationCodeId);
        probe.setCode(code);
        Page<MongoOAuth2AuthorizationCode> codes = oAuth2AuthorizationCodeRepository.findAll(Example.of(probe), pageable);
        HttpHeaders headers = HttpHeaderUtils.generatePageHeaders(codes);
        return ResponseEntity.ok().headers(headers).body(codes.getContent());
    }

    @ApiOperation("find authorization code by ID")
    @GetMapping("/api/oauth2-authorization-codes/{id}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<MongoOAuth2AuthorizationCode> findById(
            @ApiParam(value = "ID", required = true) @PathVariable String id) {
        MongoOAuth2AuthorizationCode domain = oAuth2AuthorizationCodeRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation(value = "delete authorization code by ID", notes = "the data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/oauth2-authorization-codes/{id}")
    @Secured(Authority.ADMIN)
    public ResponseEntity<Void> delete(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete oauth2 authorization code: {}", id);
        oAuth2AuthorizationCodeRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        oAuth2AuthorizationCodeRepository.deleteById(id);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1003", id))
                .build();
    }
}
