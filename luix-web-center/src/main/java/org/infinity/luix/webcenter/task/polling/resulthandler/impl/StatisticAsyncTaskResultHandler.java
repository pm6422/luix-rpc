package org.infinity.luix.webcenter.task.polling.resulthandler.impl;

import org.infinity.luix.webcenter.dto.StatisticDTO;
import org.infinity.luix.webcenter.task.polling.AsyncTask;
import org.infinity.luix.webcenter.task.polling.queue.InMemoryAsyncTaskQueue;
import org.infinity.luix.webcenter.task.polling.queue.StatisticResultHolder;
import org.infinity.luix.webcenter.task.polling.resulthandler.AsyncTaskResultHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class StatisticAsyncTaskResultHandler implements AsyncTaskResultHandler {
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void handleResult(AsyncTask asyncTask) {
        // Get message from distributed queue
        StatisticDTO result = StatisticResultHolder.get(asyncTask.getId());
        if (result != null) {
            // Set value to DeferredResult to complete the HTTP response if the specified message was found in Redis
            asyncTask.getDeferredResult().setResult(ResponseEntity.ok(result));
        } else {
            // Re-put in memory task queue if the specified message can NOT be found in Redis
            InMemoryAsyncTaskQueue.offer(asyncTask);
        }
    }
}
