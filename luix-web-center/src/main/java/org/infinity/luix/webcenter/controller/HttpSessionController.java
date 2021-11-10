package org.infinity.luix.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.webcenter.component.HttpHeaderCreator;
import org.infinity.luix.webcenter.domain.HttpSession;
import org.infinity.luix.webcenter.repository.HttpSessionRepository;
import org.infinity.luix.webcenter.utils.HttpHeaderUtils;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.exception.DataNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * REST controller for managing http sessions.
 */
@RestController
@Slf4j
public class HttpSessionController {

    @Resource
    private HttpSessionRepository httpSessionRepository;
    @Resource
    private HttpHeaderCreator     httpHeaderCreator;

    @ApiOperation("find http session list")
    @GetMapping("/api/http-sessions")
    @Secured({Authority.DEVELOPER})
    public ResponseEntity<List<HttpSession>> find(Pageable pageable,
                                                  @ApiParam(value = "principal") @RequestParam(value = "principal", required = false) String principal) {
        Page<HttpSession> sessions = StringUtils.isEmpty(principal) ? httpSessionRepository.findAll(pageable) : httpSessionRepository.findByPrincipal(pageable, principal);
        HttpHeaders headers = HttpHeaderUtils.generatePageHeaders(sessions);
        return ResponseEntity.ok().headers(headers).body(sessions.getContent());
    }

    @ApiOperation(value = "delete http session by ID", notes = "the data may be referenced by other data, and some problems may occur after deletion")
    @DeleteMapping("/api/http-sessions/{id}")
    @Secured({Authority.DEVELOPER})
    public ResponseEntity<Void> delete(@ApiParam(value = "ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete http session: {}", id);
        httpSessionRepository.findById(id).orElseThrow(() -> new DataNotFoundException(id));
        httpSessionRepository.deleteById(id);
        return ResponseEntity.ok().headers(httpHeaderCreator.createSuccessHeader("SM1003", id)).build();
    }
}
