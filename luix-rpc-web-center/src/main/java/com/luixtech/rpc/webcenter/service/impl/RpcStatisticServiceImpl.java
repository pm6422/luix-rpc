package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.webcenter.dto.StatisticDTO;
import com.luixtech.rpc.webcenter.repository.*;
import com.luixtech.rpc.webcenter.service.RpcStatisticService;
import com.luixtech.rpc.webcenter.task.polling.queue.StatisticResultHolder;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RpcStatisticServiceImpl implements RpcStatisticService {
    private final RpcApplicationRepository          rpcApplicationRepository;
    private final RpcServerRepository               rpcServerRepository;
    private final RpcServiceRepository              rpcServiceRepository;
    private final RpcProviderRepository             rpcProviderRepository;
    private final RpcConsumerRepository             rpcConsumerRepository;
    private final RpcScheduledTaskRepository        rpcScheduledTaskRepository;
    private final RpcScheduledTaskHistoryRepository rpcScheduledTaskHistoryRepository;
    private final Executor                          asyncTaskExecutor;

    /**
     * Refer to https://www.toutiao.com/a6783214804937998851/
     *
     * @param taskId task ID
     */
    @Override
    @Async
    public void getStatisticsResults(String taskId) {
        List<CompletableFuture<Long>> futures = Arrays.asList(
                CompletableFuture.supplyAsync(rpcApplicationRepository::count, asyncTaskExecutor),
                CompletableFuture.supplyAsync(() -> rpcApplicationRepository.countByActive(true), asyncTaskExecutor),

                CompletableFuture.supplyAsync(rpcServerRepository::count, asyncTaskExecutor),
                CompletableFuture.supplyAsync(() -> rpcServerRepository.countByActive(true), asyncTaskExecutor),

                CompletableFuture.supplyAsync(rpcServiceRepository::count, asyncTaskExecutor),
                CompletableFuture.supplyAsync(() -> rpcServiceRepository.countByActive(true), asyncTaskExecutor),

                CompletableFuture.supplyAsync(rpcProviderRepository::count, asyncTaskExecutor),
                CompletableFuture.supplyAsync(() -> rpcProviderRepository.countByActive(true), asyncTaskExecutor),

                CompletableFuture.supplyAsync(rpcConsumerRepository::count, asyncTaskExecutor),
                CompletableFuture.supplyAsync(() -> rpcConsumerRepository.countByActive(true), asyncTaskExecutor),

                CompletableFuture.supplyAsync(rpcScheduledTaskRepository::count, asyncTaskExecutor),
                CompletableFuture.supplyAsync(rpcScheduledTaskHistoryRepository::count, asyncTaskExecutor)
        );
        List<Long> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

        StatisticDTO dto = StatisticDTO.builder()
                .applicationCount(results.get(0))
                .activeApplicationCount(results.get(1))
                .inactiveApplicationCount(results.get(0) - results.get(1))
                .serverCount(results.get(2))
                .activeServerCount(results.get(3))
                .inactiveServerCount(results.get(2) - results.get(3))
                .serviceCount(results.get(4))
                .activeServiceCount(results.get(5))
                .inactiveServiceCount(results.get(4) - results.get(5))
                .providerCount(results.get(6))
                .activeProviderCount(results.get(7))
                .inactiveProviderCount(results.get(6) - results.get(7))
                .consumerCount(results.get(8))
                .activeConsumerCount(results.get(9))
                .inactiveConsumerCount(results.get(8) - results.get(9))
                .taskCount(results.get(10))
                .taskExecutedCount(results.get(11))
                .build();

        StatisticResultHolder.put(taskId, dto);
    }
}
