package org.infinity.luix.demoserver.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.demoserver.service.AsyncTaskService;
import org.infinity.luix.demoserver.utils.TraceIdUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

    @Override
    public String execute() {
        try {
            TimeUnit.SECONDS.sleep(5);
            return "Task " + TraceIdUtils.getTraceId() + " finished";
        } catch (InterruptedException e) {
            throw new RuntimeException();
        }
    }

    @Override
    @Async
    public void execute(DeferredResult<ResponseEntity<String>> deferred) {
        log.info(Thread.currentThread().getName() + " executing");
        try {
            TimeUnit.SECONDS.sleep(5);
            deferred.setResult(ResponseEntity.ok("Task " + TraceIdUtils.getTraceId() + " finished"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
