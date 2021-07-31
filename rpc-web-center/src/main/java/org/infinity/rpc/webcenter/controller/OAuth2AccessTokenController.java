package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.component.HttpHeaderCreator;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.domain.MongoOAuth2AccessToken;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.OAuth2AccessTokenRepository;
import org.springframework.data.domain.Example;
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
public class OAuth2AccessTokenController {

    @Resource
    private OAuth2AccessTokenRepository oAuth2AccessTokenRepository;
    @Resource
    private HttpHeaderCreator           httpHeaderCreator;

    @ApiOperation("find access token list")
    @GetMapping("/api/oauth2-access-tokens")
    @Secured(Authority.ADMIN)
    public ResponseEntity<List<MongoOAuth2AccessToken>> find(Pageable pageable,
                                                             @ApiParam(value = "access token ID") @RequestParam(value = "tokenId", required = false) String tokenId,
                                                             @ApiParam(value = "client ID") @RequestParam(value = "clientId", required = false) String clientId,
                                                             @ApiParam(value = "user name") @RequestParam(value = "userName", required = false) String userName,
                                                             @ApiParam(value = "refresh token") @RequestParam(value = "refreshToken", required = false) String refreshToken) {
        MongoOAuth2AccessToken probe = new MongoOAuth2AccessToken();
        probe.setId(tokenId);
        probe.setClientId(clientId);
        probe.setUserName(userName);
        probe.setRefreshToken(refreshToken);
        Page<MongoOAuth2AccessToken> tokens = oAuth2AccessTokenRepository.findAll(Example.of(probe), pageable);
        HttpHeaders headers = generatePageHeaders(tokens);
        return ResponseEntity.ok().headers(headers).body(tokens.getContent());
    }

    @ApiOperation("find access token by ID")
    @GetMapping("/api/oauth2-access-tokens/{id}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<MongoOAuth2AccessToken> findById(
            @ApiParam(value = "ID", required = true) @PathVariable String id) {
        MongoOAuth2AccessToken domain = oAuth2AccessTokenRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation(value = "delete access token by ID", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/oauth2-access-tokens/{id}")
    @Secured(Authority.ADMIN)
    public ResponseEntity<Void> delete(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete oauth2 access token: {}", id);
        oAuth2AccessTokenRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        oAuth2AccessTokenRepository.deleteById(id);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1003", id)).build();
    }
}
