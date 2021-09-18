package org.infinity.luix.webcenter.service.impl;

import org.infinity.luix.webcenter.dto.StatisticsDTO;
import org.infinity.luix.webcenter.repository.*;
import org.infinity.luix.webcenter.service.StatisticsService;
import org.infinity.luix.webcenter.task.polling.queue.StatisticsResultQueue;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class StatisticsServiceImpl implements StatisticsService {

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

    @Override
    @Async
    public void getStatistics(String taskId) {
        StatisticsDTO dto = StatisticsDTO.builder()
                .applicationCount(rpcApplicationRepository.count())
                .serverCount(rpcServerRepository.count())
                .serviceCount(rpcServiceRepository.count())
                .providerCount(rpcProviderRepository.count())
                .consumerCount(rpcConsumerRepository.count())
                .taskCount(rpcScheduledTaskRepository.count())
                .taskExecutedCount(rpcScheduledTaskHistoryRepository.count())
                .build();

        StatisticsResultQueue.put(taskId, dto);
    }
}
