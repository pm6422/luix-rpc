package org.infinity.rpc.webcenter.controller;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.utilities.id.IdGenerator;
import org.infinity.rpc.webcenter.component.HttpHeaderCreator;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.domain.MongoOAuth2ClientDetails;
import org.infinity.rpc.webcenter.exception.DuplicationException;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.OAuth2ClientDetailsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

@RestController
@Slf4j
public class OAuth2ClientDetailsController {

    @Resource
    private OAuth2ClientDetailsRepository oAuth2ClientDetailsRepository;
    @Resource
    private MongoTemplate                 mongoTemplate;
    @Resource
    private HttpHeaderCreator             httpHeaderCreator;
    @Resource
    private PasswordEncoder               passwordEncoder;

    @ApiOperation("create oauth client")
    @PostMapping("/api/oauth2-clients")
    @Secured(Authority.ADMIN)
    public ResponseEntity<Void> create(
            @ApiParam(value = "oauth client", required = true) @Valid @RequestBody MongoOAuth2ClientDetails domain) {
        log.debug("REST create oauth client detail: {}", domain);
        domain.setClientId(StringUtils.defaultIfEmpty(domain.getClientId(), "" + IdGenerator.generateTimestampId()));
        oAuth2ClientDetailsRepository.findById(domain.getClientId()).ifPresent((existingEntity) -> {
            throw new DuplicationException(ImmutableMap.of("clientId", domain.getClientId()));
        });
        domain.setRawClientSecret(StringUtils.defaultIfEmpty(domain.getClientSecret(), "" + IdGenerator.generateTimestampId()));
        domain.setClientSecret(passwordEncoder.encode(domain.getRawClientSecret()));
        oAuth2ClientDetailsRepository.save(domain);
        return ResponseEntity.status(HttpStatus.CREATED)
                .headers(httpHeaderCreator.createSuccessHeader("SM1001", domain.getClientId()))
                .build();
    }

    @ApiOperation("find oauth client list")
    @GetMapping("/api/oauth2-clients")
    @Secured(Authority.ADMIN)
    public ResponseEntity<List<MongoOAuth2ClientDetails>> find(Pageable pageable,
                                                               @ApiParam(value = "ID") @RequestParam(value = "clientId", required = false) String clientId) {
        Query query = Query.query(Criteria.where("clientId").is(clientId));
        long totalCount = mongoTemplate.count(query, MongoOAuth2ClientDetails.class);
        query.with(pageable);// Note: the field name
        Page<MongoOAuth2ClientDetails> clientDetails = StringUtils.isEmpty(clientId)
                ? oAuth2ClientDetailsRepository.findAll(pageable)
                : new PageImpl<>(mongoTemplate.find(query, MongoOAuth2ClientDetails.class), pageable, totalCount);
        HttpHeaders headers = generatePageHeaders(clientDetails);
        return ResponseEntity.ok().headers(headers).body(clientDetails.getContent());
    }

    @ApiOperation("find oauth client by ID")
    @GetMapping("/api/oauth2-clients/{id}")
    @Secured({Authority.ADMIN})
    public ResponseEntity<MongoOAuth2ClientDetails> findById(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        MongoOAuth2ClientDetails domain = oAuth2ClientDetailsRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        return ResponseEntity.ok(domain);
    }

    @ApiOperation("find oauth internal client")
    @GetMapping("/open-api/oauth2-client/internal-client")
    public ResponseEntity<Pair<String, String>> findInternalClient() {
        return ResponseEntity.ok(Pair.of(MongoOAuth2ClientDetails.INTERNAL_CLIENT_ID,
                MongoOAuth2ClientDetails.INTERNAL_RAW_CLIENT_SECRET));
    }

    @ApiOperation("update oauth client")
    @PutMapping("/api/oauth2-clients")
    @Secured(Authority.ADMIN)
    public ResponseEntity<Void> update(
            @ApiParam(value = "new oauth client", required = true) @Valid @RequestBody MongoOAuth2ClientDetails domain) {
        log.debug("REST request to update oauth client detail: {}", domain);
        oAuth2ClientDetailsRepository.findById(domain.getClientId()).orElseThrow(() -> new NoDataFoundException(domain.getClientId()));
        oAuth2ClientDetailsRepository.save(domain);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1002", domain.getClientId()))
                .build();

    }

    @ApiOperation(value = "delete oauth client by ID", notes = "The data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/oauth2-clients/{id}")
    @Secured(Authority.ADMIN)
    public ResponseEntity<Void> delete(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete oauth client detail: {}", id);
        oAuth2ClientDetailsRepository.findById(id).orElseThrow(() -> new NoDataFoundException(id));
        oAuth2ClientDetailsRepository.deleteById(id);
        return ResponseEntity.ok()
                .headers(httpHeaderCreator.createSuccessHeader("SM1003", id)).build();
    }
}
