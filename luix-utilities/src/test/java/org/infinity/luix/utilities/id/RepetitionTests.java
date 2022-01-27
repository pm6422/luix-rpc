package org.infinity.luix.utilities.id;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.utilities.collection.ConcurrentHashSet;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 重复性测试
 */
@Slf4j
public class RepetitionTests {
    private static final SnowFlakeIdGenerator SNOW_FLAKE_ID_GENERATOR = new SnowFlakeIdGenerator(1L, false, false);

    /**
     * single thread test
     *
     * @throws InterruptedException
     */
    @Test
    public void testSingleThread() throws InterruptedException {
        // Thread-safe container
        Set<Long> set = new ConcurrentHashSet<>();
        int maxTimes = 10000 * 10;
        SnowFlakeIdGenerator snowFlakeIdGenerator = new SnowFlakeIdGenerator(1L, false, false);
        // Single thread
        ExecutorService threadPool = Executors.newFixedThreadPool(1);

        IntStream.range(0, maxTimes).forEach(i -> {
            threadPool.execute(() -> {
                log.debug("Active thread count: {}", Thread.activeCount());
                set.add(snowFlakeIdGenerator.nextId());
            });
        });

        threadPool.shutdown();
        if (threadPool.awaitTermination(1, TimeUnit.HOURS)) {
            assertThat(maxTimes).isEqualTo(set.size());
        }
    }

    /**
     * Can guarantee unique on single-threads environment
     *
     * @throws InterruptedException
     */
    /**
     * Can guarantee unique on multi-threads environment
     *
     * @throws InterruptedException
     */
    @Test
    public void multiThreadUniqueTestForSnowFlakeId() throws InterruptedException {
        // Thread-safe container
        Set<Long> set = new ConcurrentHashSet<>();
        int maxTimes = 10000 * 10;

        // Multi-threads
        ExecutorService threadPool = Executors.newFixedThreadPool(8);

        IntStream.range(0, maxTimes).forEach(i -> {
            threadPool.execute(() -> {
                long requestId = SNOW_FLAKE_ID_GENERATOR.nextId();
                System.out.println(requestId);
                set.add(requestId);
            });
        });

        threadPool.shutdown();
        if (threadPool.awaitTermination(1, TimeUnit.HOURS)) {
            // equals
            assertThat(maxTimes).isEqualTo(set.size());
        }
    }

    /**
     * Can guarantee unique on multi-threads environment
     *
     * @throws InterruptedException
     */
    @Test
    public void multiThreadUniqueTestForTimestampId() throws InterruptedException {
        // Thread-safe container
        Set<Long> set = new ConcurrentHashSet<>();
        int maxTimes = 10000 * 10;

        // Multi-threads
        ExecutorService threadPool = Executors.newFixedThreadPool(8);

        IntStream.range(0, maxTimes).forEach(i -> {
            threadPool.execute(() -> {
                long requestId = IdGenerator.generateTimestampId();
                System.out.println(requestId);
                set.add(requestId);
            });
        });

        threadPool.shutdown();
        if (threadPool.awaitTermination(1, TimeUnit.HOURS)) {
            // equals
            assertThat(maxTimes).isEqualTo(set.size());
        }
    }

    /**
     * Can guarantee unique by using short id generator on multi-threads environment
     *
     * @throws InterruptedException
     */
    @Test
    public void multiThreadUniqueTestForShortId() throws InterruptedException {
        // thread-safe container
        Set<Long> set = new ConcurrentHashSet<>();
        int maxTimes = 100 * 10;

        // Multi-threads
        ExecutorService threadPool = Executors.newFixedThreadPool(8);

        IntStream.range(0, maxTimes).forEach(i -> {
            threadPool.execute(() -> {
                long requestId = IdGenerator.generateShortId();
                set.add(requestId);
            });
        });

        threadPool.shutdown();
        if (threadPool.awaitTermination(1, TimeUnit.HOURS)) {
            // equals
            assertThat(maxTimes).isEqualTo(set.size());
        }
    }

    @Test
    public void testOnMultipleDataCenters() {
        Set<Long> set = new HashSet<>();
        SnowFlakeIdGenerator sequence1 = new SnowFlakeIdGenerator(0);
        SnowFlakeIdGenerator sequence2 = new SnowFlakeIdGenerator(1);
        Thread t1 = new Thread(new IdWorkThread(set, sequence1));
        Thread t2 = new Thread(new IdWorkThread(set, sequence2));
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class IdWorkThread implements Runnable {
        private Set<Long>            set;
        private SnowFlakeIdGenerator idWorker;

        public IdWorkThread(Set<Long> set, SnowFlakeIdGenerator idWorker) {
            this.set = set;
            this.idWorker = idWorker;
        }

        @Override
        public void run() {
            while (true) {
                long id = idWorker.nextId();
                if (!set.add(id)) {
                    System.out.println("Found duplicated id:" + id);
                }
            }
        }
    }
}