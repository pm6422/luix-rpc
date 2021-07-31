package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.component.HttpHeaderCreator;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.domain.MongoOAuth2RefreshToken;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.OAuth2RefreshTokenRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class OAuth2RefreshTokenController {

    @Resource
    private OAuth2RefreshTokenRepository oAuth2RefreshTokenRepository;
    @Resource
    private HttpHeaderCreator            httpHeaderCreator;

    @ApiOperation("find refresh token list")
    @GetMapping("/api/oauth2-refresh-tokens")
    @Secured(Authority.ADMIN)
    public ResponseEntity<List<MongoOAuth2RefreshToken>> find(Pageable pageable,
                                                              @ApiParam(value = "refresh token ID") @RequestParam(value = "tokenId", required = false) String tokenId,
                                                              @ApiParam(value = "client ID") @RequestParam(value = "clientId", required = false) String clientId,
                                                              @ApiParam(value = "user name") @RequestParam(value = "userName", required = false) String userName) {
        MongoOAuth2RefreshToken probe = new MongoOAuth2RefreshToken();
        probe.setId(tokenId);
        probe.setClientId(clientId);
        probe.setUserName(userName);
        // Ignore query parameter if it has a null value
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues();
        Page<MongoOAuth2RefreshToken> tokens = oAuth2RefreshTokenRepository.findAll(Example.of(probe, matcher), pageable);
        HttpHeaders headers = generatePageHeaders(tokens);
        return ResponseEntity.ok().headers(headers).body(tokens.getContent());
    }

    @ApiOperation("find refresh token by ID")
    @GetMapping("/api/oauth2-refresh-tokens/{id}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<MongoOAuth2RefreshToken> findById(
            @ApiParam(value = "ID", required = true) @PathVariable String id) {
        MongoOAuth2RefreshToken domain = oAuth2RefreshTokenRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation(value = "delete refresh token by ID", notes = "the data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/oauth2-refresh-tokens/{id}")
    @Secured(Authority.ADMIN)
    public ResponseEntity<Void> delete(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete oauth2 access token: {}", id);
        oAuth2RefreshTokenRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        oAuth2RefreshTokenRepository.deleteById(id);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1003", id))
                .build();
    }
}
