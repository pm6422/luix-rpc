package org.infinity.luix.webcenter.task.polling.resulthandler.impl;

import org.infinity.luix.webcenter.dto.StatisticsDTO;
import org.infinity.luix.webcenter.task.polling.queue.AsyncTask;
import org.infinity.luix.webcenter.task.polling.queue.InMemoryAsyncTaskQueue;
import org.infinity.luix.webcenter.task.polling.queue.StatisticsResultQueue;
import org.infinity.luix.webcenter.task.polling.resulthandler.AsyncTaskResultHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class StatisticsAsyncTaskResultHandler implements AsyncTaskResultHandler {
    @Override
    public void handle(AsyncTask asyncTask) {
        // Get message from distributed queue
        StatisticsDTO result = StatisticsResultQueue.get(asyncTask.getId());
        if (result != null) {
            // Set value to DeferredResult to complete the HTTP response if the specified message was found in Redis
            asyncTask.getDeferredResult().setResult(ResponseEntity.ok(result));
        } else {
            // Re-put in memory task queue if the specified message can NOT be found in Redis
            InMemoryAsyncTaskQueue.offer(asyncTask);
        }
    }
}
