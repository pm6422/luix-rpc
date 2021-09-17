package org.infinity.luix.demoserver.task.polling.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
public abstract class InMemoryDeferredTaskQueue {
    private static final int                      QUEUE_LENGTH = 100000;
    /**
     * 有界阻塞队列。数组结构固定大小的阻塞队列，大小由构造函数的参数指定
     * ArrayBlockingQueue中只有一个ReentrantLock对象，读和写都需要获取锁，这意味着生产者和消费者无法并行运行。
     * 当队列容量满时尝试将元素放入队列将导致操作阻塞，尝试从一个空队列中取一个元素也会同样阻塞。
     */
    private static final BlockingQueue<AsyncTask> QUEUE        = new ArrayBlockingQueue<>(QUEUE_LENGTH);

    /**
     * Inserts the specified element into this queue
     *
     * @param id             task ID
     * @param deferredResult deferred result object
     * @return returning true upon success and false if no space is currently available
     */
    public static boolean offer(String id, DeferredResult<ResponseEntity<String>> deferredResult) {
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

