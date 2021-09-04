package org.infinity.luix.core.thread;

import org.apache.commons.lang3.Validate;
import org.infinity.luix.utilities.destory.ShutdownHook;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class ScheduledThreadPool {
    public static final String RETRY_THREAD_POOL                      = "RETRY_THREAD_POOL";
    public static final String CALCULATE_MEMORY_THREAD_POOL           = "CALCULATE_MEMORY_THREAD_POOL";
    public static final int    CALCULATE_MEMORY_INTERVAL              = 60000;
    public static final String CHECK_HEALTH_THREAD_POOL               = "CHECK_HEALTH_THREAD_POOL";
    public static final int    CHECK_HEALTH_INTERVAL                  = 500;
    public static final String DESTROY_SENDER_THREAD_POOL             = "DESTROY_SENDER_THREAD_POOL";
    public static final int    DESTROY_SENDER_DELAY                   = 1000;
    public static final String DESTROY_NETTY_TIMEOUT_TASK_THREAD_POOL = "DESTROY_NETTY_TIMEOUT_THREAD_POOL";
    public static final int    DESTROY_NETTY_TIMEOUT_INTERVAL         = 100;

    private static final Map<String, ScheduledExecutorService> THREAD_POOL_MAP = new HashMap<>();

    static {
        THREAD_POOL_MAP.put(RETRY_THREAD_POOL, Executors.newScheduledThreadPool(1));
        THREAD_POOL_MAP.put(CALCULATE_MEMORY_THREAD_POOL, Executors.newScheduledThreadPool(1));
        THREAD_POOL_MAP.put(CHECK_HEALTH_THREAD_POOL, Executors.newScheduledThreadPool(1));
        THREAD_POOL_MAP.put(DESTROY_SENDER_THREAD_POOL, Executors.newScheduledThreadPool(1));
        THREAD_POOL_MAP.put(DESTROY_NETTY_TIMEOUT_TASK_THREAD_POOL, Executors.newScheduledThreadPool(1));

        // Destroy the thread pools when the system exits
        ShutdownHook.add(() -> {
            for (ScheduledExecutorService threadPool : THREAD_POOL_MAP.values()) {
                if (!threadPool.isShutdown()) {
                    threadPool.shutdown();
                }
            }
        });
    }

    /**
     * Schedule a task to run at periodic intervals
     *
     * @param threadPoolName thread pool name
     * @param interval       execution interval
     * @param task           task to be executed
     * @return result
     */
    public static ScheduledFuture<?> schedulePeriodicalTask(String threadPoolName, long interval, Runnable task) {
        return schedulePeriodicalTask(threadPoolName, interval, interval, TimeUnit.MILLISECONDS, task);
    }

    /**
     * Schedule a task to run at periodic intervals
     *
     * @param threadPoolName thread pool name
     * @param initialDelay   initial delay
     * @param interval       execution interval
     * @param timeUnit       time unit
     * @param task           task to be executed
     * @return result
     */
    public static ScheduledFuture<?> schedulePeriodicalTask(String threadPoolName, long initialDelay,
                                                            long interval, TimeUnit timeUnit, Runnable task) {
        Validate.isTrue(THREAD_POOL_MAP.containsKey(threadPoolName), "Please specify a valid thread pool name!");

        // 以上一个任务开始的时间计时，period时间过去后，检测上一个任务是否执行完毕，
        // 如果上一个任务执行完毕，则当前任务立即执行，如果上一个任务没有执行完毕，则需要等上一个任务执行完毕后立即执行。
        return THREAD_POOL_MAP.get(threadPoolName).scheduleAtFixedRate(task, initialDelay, interval, timeUnit);
    }

    /**
     * Execute once after a daley time
     *
     * @param threadPoolName thread pool name
     * @param delay          delay time in milliseconds
     * @param task           task to be executed
     * @return thread pool
     */
    public static ScheduledExecutorService scheduleDelayTask(String threadPoolName, long delay, Runnable task) {
        return scheduleDelayTask(threadPoolName, delay, TimeUnit.MILLISECONDS, task);
    }

    /**
     * Execute once after a daley time
     *
     * @param threadPoolName thread pool name
     * @param delay          delay time in milliseconds
     * @param timeUnit       time unit
     * @param task           task to be executed
     * @return thread pool
     */
    public static ScheduledExecutorService scheduleDelayTask(String threadPoolName, long delay,
                                                             TimeUnit timeUnit, Runnable task) {
        Validate.isTrue(THREAD_POOL_MAP.containsKey(threadPoolName), "Please specify a valid thread pool name!");

        ScheduledExecutorService scheduledExecutorService = THREAD_POOL_MAP.get(threadPoolName);
        scheduledExecutorService.schedule(task, delay, timeUnit);
        return scheduledExecutorService;
    }

    public static void shutdownNow(String threadPoolName) {
        Validate.isTrue(THREAD_POOL_MAP.containsKey(threadPoolName), "Please specify a valid thread pool name!");
        THREAD_POOL_MAP.get(threadPoolName).shutdownNow();
    }
}
