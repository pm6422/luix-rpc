package org.infinity.luix.demoserver.task.polling.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
@Component
public class InMemoryTaskQueue {
    private static final int                 QUEUE_LENGTH = 10;
    private final        BlockingQueue<Task> queue        = new LinkedBlockingDeque<>(QUEUE_LENGTH);

    public void put(String id, DeferredResult<ResponseEntity<String>> deferredResult) {
        queue.offer(Task.builder().id(id).deferredResult(deferredResult).build());
    }

    public void put(Task task) {
        queue.offer(task);
    }

    public Task take() throws InterruptedException {
        return queue.poll();
    }
}

