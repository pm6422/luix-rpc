package org.infinity.rpc.utilities.id;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 重复性测试
 */
@Slf4j
public class RepetitionTest {
    /**
     * single thread test
     *
     * @throws InterruptedException
     */
    @Test
    public void singleThreadTest() throws InterruptedException {
        // thread-safe container
        Set<Long> set = new ConcurrentHashSet<>();
        int maxTimes = 10000 * 10;
        Sequence sequence = new Sequence(1L, false, false);
        // Single thread
        ExecutorService threadPool = Executors.newFixedThreadPool(1);

        IntStream.range(0, maxTimes).forEach(i -> {
            threadPool.execute(() -> {
                log.debug("Active thread count: {}", Thread.activeCount());
                set.add(sequence.nextId());
            });
        });

        threadPool.shutdown();
        if (threadPool.awaitTermination(1, TimeUnit.HOURS)) {
            Assert.assertEquals(maxTimes, set.size());
        }
    }

    /**
     * Can guarantee unique on single-threads environment
     *
     * @throws InterruptedException
     */
    @Test
    public void multiThreadUniqueTest() throws InterruptedException {
        // thread-safe container
        Set<Long> set = new ConcurrentHashSet<>();
        int maxTimes = 10000 * 10;
        Sequence sequence = new Sequence(1L, false, false);
        // Multi-threads
        ExecutorService threadPool = Executors.newFixedThreadPool(4);

        IntStream.range(0, maxTimes).forEach(i -> {
            threadPool.execute(() -> {
                log.debug("Active thread count: {}", Thread.activeCount());
                set.add(sequence.nextId());
            });
        });

        threadPool.shutdown();
        if (threadPool.awaitTermination(1, TimeUnit.HOURS)) {
            // not equal
            Assert.assertNotEquals(maxTimes, set.size());
        }
    }

    /**
     * Can guarantee unique on multi-threads environment
     *
     * @throws InterruptedException
     */
    @Test
    public void multiThreadThreadSafeTest() throws InterruptedException {
        // thread-safe container
        Set<Long> set = new ConcurrentHashSet<>();
        int maxTimes = 10000 * 10;
        // Multi-threads
        ExecutorService threadPool = Executors.newFixedThreadPool(8);

        IntStream.range(0, maxTimes).forEach(i -> {
            threadPool.execute(() -> {
                long requestId = RequestIdGenerator.getRequestId();
                System.out.println(requestId);
                set.add(requestId);
            });
        });

        threadPool.shutdown();
        if (threadPool.awaitTermination(1, TimeUnit.HOURS)) {
            // equals
            Assert.assertEquals(maxTimes, set.size());
        }
    }
}