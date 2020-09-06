package org.infinity.rpc.core.destroy;

import org.apache.commons.lang3.Validate;
import org.infinity.rpc.utilities.destory.ShutdownHook;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledDestroyThreadPool {
    public static final String RETRY_THREAD_POOL          = "RETRY_THREAD_POOL";
    public static final String DESTROY_CALLER_THREAD_POOL = "DESTROY_CALLER_THREAD_POOL";

    private static final Map<String, ScheduledExecutorService> THREAD_POOL_MAP = new HashMap<>();

    static {
        THREAD_POOL_MAP.put(RETRY_THREAD_POOL, Executors.newScheduledThreadPool(1));
        THREAD_POOL_MAP.put(DESTROY_CALLER_THREAD_POOL, Executors.newScheduledThreadPool(1));
    }

    /**
     * Clean up the thread pool when the system exit
     */
    static {
        ShutdownHook.add(() -> {
            for (ScheduledExecutorService threadPool : THREAD_POOL_MAP.values()) {
                if (!threadPool.isShutdown()) {
                    threadPool.shutdown();
                }
            }
        });
    }

    public static void schedulePeriodicalTask(String threadPoolName, long initialDelay, long period, TimeUnit timeUnit, Runnable command) {
        Validate.isTrue(THREAD_POOL_MAP.keySet().contains(threadPoolName), "Please specify a valid thread pool name!");

        // Redo at retry interval periodically
        THREAD_POOL_MAP.get(threadPoolName).scheduleAtFixedRate(() -> {
            // Do the task
            command.run();
        }, initialDelay, period, timeUnit);
    }

    public static void scheduleDelayTask(String threadPoolName, long delay, TimeUnit timeUnit, Runnable command) {
        Validate.isTrue(THREAD_POOL_MAP.keySet().contains(threadPoolName), "Please specify a valid thread pool name!");

        // Execute once after a daley time
        THREAD_POOL_MAP.get(threadPoolName).schedule(() -> {
            // Do the task
            command.run();
        }, delay, timeUnit);
    }
}
