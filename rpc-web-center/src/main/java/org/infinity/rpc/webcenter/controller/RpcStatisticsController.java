package org.infinity.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.webcenter.dto.StatisticsDTO;
import org.infinity.rpc.webcenter.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
public class RpcStatisticsController {

    @Resource
    private RpcApplicationRepository          rpcApplicationRepository;
    @Resource
    private RpcServerRepository               rpcServerRepository;
    @Resource
    private RpcServiceRepository              rpcServiceRepository;
    @Resource
    private RpcProviderRepository             rpcProviderRepository;
    @Resource
    private RpcConsumerRepository             rpcConsumerRepository;
    @Resource
    private RpcScheduledTaskRepository        rpcScheduledTaskRepository;
    @Resource
    private RpcScheduledTaskHistoryRepository rpcScheduledTaskHistoryRepository;

    @ApiOperation("get RPC statistics data")
    @GetMapping("api/rpc-statistics/data")
    @Timed
    public ResponseEntity<StatisticsDTO> getStatistics() {
        StatisticsDTO dto = StatisticsDTO.builder()
                .applicationCount(rpcApplicationRepository.count())
                .serverCount(rpcServerRepository.count())
                .serviceCount(rpcServiceRepository.count())
                .providerCount(rpcProviderRepository.count())
                .consumerCount(rpcConsumerRepository.count())
                .taskCount(rpcScheduledTaskRepository.count())
                .taskExecutedCount(rpcScheduledTaskHistoryRepository.count())
                .build();

        return ResponseEntity.ok(dto);
    }
}
