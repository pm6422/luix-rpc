package org.infinity.luix.webcenter.service.impl;

import org.infinity.luix.webcenter.dto.StatisticsDTO;
import org.infinity.luix.webcenter.repository.*;
import org.infinity.luix.webcenter.service.RpcStatisticsService;
import org.infinity.luix.webcenter.task.polling.queue.StatisticsResultQueue;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class RpcStatisticsServiceImpl implements RpcStatisticsService {

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
    @Resource
    private Executor                          asyncTaskExecutor;

    /**
     * Refer to https://www.toutiao.com/a6783214804937998851/
     *
     * @param taskId task ID
     */
    @Override
    @Async
    public void getStatistics(String taskId) {
        List<CompletableFuture<Long>> futures = new ArrayList<>(7);
        futures.add(CompletableFuture.supplyAsync(rpcApplicationRepository::count, asyncTaskExecutor));
        futures.add(CompletableFuture.supplyAsync(rpcServerRepository::count, asyncTaskExecutor));
        futures.add(CompletableFuture.supplyAsync(rpcServiceRepository::count, asyncTaskExecutor));
        futures.add(CompletableFuture.supplyAsync(rpcProviderRepository::count, asyncTaskExecutor));
        futures.add(CompletableFuture.supplyAsync(rpcConsumerRepository::count, asyncTaskExecutor));
        futures.add(CompletableFuture.supplyAsync(rpcScheduledTaskRepository::count, asyncTaskExecutor));
        futures.add(CompletableFuture.supplyAsync(rpcScheduledTaskHistoryRepository::count, asyncTaskExecutor));

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[7]));
        CompletableFuture<List<Long>> listCompletableFuture = allFutures
                .thenApplyAsync(val -> futures.stream().map(CompletableFuture::join)
                        .collect(Collectors.toList()), asyncTaskExecutor);
        List<Long> results = listCompletableFuture.join();

        StatisticsDTO dto = StatisticsDTO.builder()
                .applicationCount(results.get(0))
                .serverCount(results.get(1))
                .serviceCount(results.get(2))
                .providerCount(results.get(3))
                .consumerCount(results.get(4))
                .taskCount(results.get(5))
                .taskExecutedCount(results.get(6))
                .build();

        StatisticsResultQueue.put(taskId, dto);
    }
}
