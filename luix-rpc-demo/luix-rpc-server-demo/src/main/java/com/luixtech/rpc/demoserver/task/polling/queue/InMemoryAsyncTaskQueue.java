package com.luixtech.rpc.demoserver.task.polling.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * An in-memory queue used to store {@link org.springframework.web.context.request.async.DeferredResult}
 */
@Slf4j
public abstract class InMemoryAsyncTaskQueue {
    private static final int                      QUEUE_SIZE = 100000;
    /**
     * Bounded blocking queue, and the size is specified by the argument value of the constructor.
     * There is only one ReentrantLock object in ArrayBlockingQueue. Both reading and writing need to obtain locks,
     * which means that producing and consuming cannot run in parallel.
     * When the queue is full, trying to put an element in the queue will cause the operation to block,
     * and trying to get an element from an empty queue will also block.
     */
    private static final BlockingQueue<AsyncTask> QUEUE      = new ArrayBlockingQueue<>(QUEUE_SIZE);

    /**
     * Inserts the specified element into this queue
     *
     * @param id             task ID
     * @param deferredResult deferred result object
     * @return returning true upon success and false if no space is currently available
     */
    public static boolean offer(String id, DeferredResult<ResponseEntity<String>> deferredResult) {
        log.info("InMemory deferred task queue remaining capacity: [{}]", QUEUE.remainingCapacity());
        return QUEUE.offer(AsyncTask.builder().id(id).deferredResult(deferredResult).build());
    }

    /**
     * Inserts the specified element into this queue
     *
     * @param asyncTask task to be inserted
     * @return returning true upon success and false if no space is currently available
     */
    public static boolean offer(AsyncTask asyncTask) {
        return QUEUE.offer(asyncTask);
    }

    /**
     * Retrieves and removes the head of this queue
     *
     * @return the head of this queue, or null if this queue is empty
     * @throws InterruptedException if any exception occurs
     */
    public static AsyncTask poll() throws InterruptedException {
        return QUEUE.poll();
    }
}

