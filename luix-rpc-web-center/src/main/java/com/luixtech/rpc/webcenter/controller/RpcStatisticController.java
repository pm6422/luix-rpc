package com.luixtech.rpc.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import com.luixtech.rpc.webcenter.dto.StatisticDTO;
import com.luixtech.rpc.webcenter.service.RpcStatisticService;
import com.luixtech.rpc.webcenter.task.polling.queue.InMemoryAsyncTaskQueue;
import com.luixtech.rpc.webcenter.utils.TraceIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.luixtech.rpc.webcenter.config.api.SpringDocConfiguration.AUTH;
import static com.luixtech.rpc.webcenter.task.polling.AsyncTask.NAME_STATISTIC;
import static org.springframework.http.HttpStatus.*;

@RestController
@SecurityRequirement(name = AUTH)
@Slf4j
public class RpcStatisticController {

    @Resource
    private RpcStatisticService rpcStatisticService;

    @Operation(summary = "get RPC statistic data")
    @GetMapping("api/rpc-statistics/data")
    @Timed
    public DeferredResult<ResponseEntity<StatisticDTO>> getStatistics() {
        DeferredResult<ResponseEntity<StatisticDTO>> deferredResult = new DeferredResult<>(TimeUnit.MINUTES.toMillis(1));
        handleAsyncError(deferredResult);

        // Put task in memory queue
        boolean hasCapacity = InMemoryAsyncTaskQueue.offer(TraceIdUtils.getTraceId(), NAME_STATISTIC, deferredResult);
        if (!hasCapacity) {
            // If the ArrayBlockingQueue is full
            deferredResult.setErrorResult(ResponseEntity.status(FORBIDDEN).body("Server is busy!"));
        } else {
            // Execute asynchronously
            rpcStatisticService.getStatisticsResults(TraceIdUtils.getTraceId());
        }
        return deferredResult;
    }

    private <T> void handleAsyncError(DeferredResult<ResponseEntity<T>> deferredResult) {
        // Handle timeout
        deferredResult.onTimeout(() ->
                deferredResult.setErrorResult(
                        ResponseEntity.status(REQUEST_TIMEOUT).body("Request timeout!")));
        // Handle error
        deferredResult.onError((Throwable t) -> deferredResult.setErrorResult(
                ResponseEntity.status(INTERNAL_SERVER_ERROR).body(t.getMessage())));
    }
}
