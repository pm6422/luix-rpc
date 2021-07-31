package org.infinity.rpc.webcenter.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.infinity.rpc.webcenter.domain.Authority;
import org.infinity.rpc.webcenter.domain.PersistentAuditEvent;
import org.infinity.rpc.webcenter.repository.PersistenceAuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

import static org.infinity.rpc.webcenter.utils.HttpHeaderUtils.generatePageHeaders;

/**
 * REST controller for managing the user audit events.
 */
@RestController
public class UserAuditEventController {

    @Resource
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    /**
     * 分页检索用户审计事件列表
     *
     * @param pageable 分页信息
     * @param from     开始日期 Instant反序列化会发生错误，所以使用LocalDate
     * @param to       结束日期 Instant反序列化会发生错误，所以使用LocalDate
     * @return 分页信息
     */
    @ApiOperation("find user audit list")
    @GetMapping("/api/user-audit-events")
    @Secured(Authority.DEVELOPER)
    public ResponseEntity<List<PersistentAuditEvent>> getUserAuditEvents(Pageable pageable,
                                                                         @ApiParam(value = "start date，e.g：2020-10-01") @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                         @ApiParam(value = "end date，e.g：2020-10-02") @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Page<PersistentAuditEvent> userAuditEvents = persistenceAuditEventRepository.findByAuditEventDateBetween(pageable, from, to);
        HttpHeaders headers = generatePageHeaders(userAuditEvents);
        return ResponseEntity.ok().headers(headers).body(userAuditEvents.getContent());
    }
}
