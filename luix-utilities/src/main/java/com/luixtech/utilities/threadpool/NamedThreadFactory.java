package com.luixtech.utilities.threadpool;


import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private static final AtomicInteger THREAD_POOL_NUM     = new AtomicInteger(1);
    private final        AtomicInteger currentThreadNumber = new AtomicInteger(1);
    private final        ThreadGroup   threadGroup;
    private final        String        threadPoolName;
    private final        int           priority;
    private final        boolean       isDaemon;

    public NamedThreadFactory(String threadPoolNamePrefix) {
        this(threadPoolNamePrefix, false);
    }

    public NamedThreadFactory(String threadPoolNamePrefix, boolean isDaemon) {
        this(threadPoolNamePrefix, isDaemon, Thread.NORM_PRIORITY);
    }

    public NamedThreadFactory(String threadPoolNamePrefix, boolean isDaemon, int priority) {
        SecurityManager securityManager = System.getSecurityManager();
        this.threadGroup = (securityManager != null) ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.threadPoolName = threadPoolNamePrefix + "-" + THREAD_POOL_NUM.getAndIncrement() + "-thread-";
        this.isDaemon = isDaemon;
        this.priority = priority;
    }

    @Override
    public Thread newThread(@Nonnull Runnable r) {
        Thread thread = new Thread(threadGroup, r, threadPoolName + currentThreadNumber.getAndIncrement(), 0);
        thread.setDaemon(isDaemon);
        thread.setPriority(priority);
        return thread;
    }
}
