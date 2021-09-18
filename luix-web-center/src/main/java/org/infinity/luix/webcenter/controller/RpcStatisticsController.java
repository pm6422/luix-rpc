package org.infinity.luix.webcenter.controller;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.webcenter.dto.StatisticsDTO;
import org.infinity.luix.webcenter.service.RpcStatisticsService;
import org.infinity.luix.webcenter.task.polling.queue.InMemoryAsyncTaskQueue;
import org.infinity.luix.webcenter.utils.TraceIdUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;

import static org.springframework.http.HttpStatus.*;

@RestController
@Slf4j
public class RpcStatisticsController {

    @Resource
    private RpcStatisticsService rpcStatisticsService;

    @ApiOperation("get RPC statistics data")
    @GetMapping("api/rpc-statistics/data")
    @Timed
    public DeferredResult<ResponseEntity<StatisticsDTO>> getStatistics() {
        DeferredResult<ResponseEntity<StatisticsDTO>> deferredResult = new DeferredResult<>(2000L);
        handleAsyncError(deferredResult);

        // Put task in memory queue
        boolean hasCapacity = InMemoryAsyncTaskQueue.offer(TraceIdUtils.getTraceId(), "statistics", deferredResult);
        if (!hasCapacity) {
            // If the ArrayBlockingQueue is full
            deferredResult.setErrorResult(ResponseEntity.status(FORBIDDEN).body("Server is busy!"));
        } else {
            // Execute asynchronously
            rpcStatisticsService.getStatistics(TraceIdUtils.getTraceId());
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
