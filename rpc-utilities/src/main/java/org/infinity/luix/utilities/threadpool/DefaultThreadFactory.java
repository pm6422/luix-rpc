/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.infinity.luix.utilities.threadpool;


import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadFactory implements ThreadFactory {
    private static final AtomicInteger THREAD_POOL_NUM     = new AtomicInteger(1);
    private final        AtomicInteger currentThreadNumber = new AtomicInteger(1);
    private final        ThreadGroup   threadGroup;
    private final        String        threadPoolName;
    private final        int           priority;
    private final        boolean       isDaemon;

    public DefaultThreadFactory(String threadPoolNamePrefix) {
        this(threadPoolNamePrefix, false);
    }

    public DefaultThreadFactory(String threadPoolNamePrefix, boolean isDaemon) {
        this(threadPoolNamePrefix, isDaemon, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(String threadPoolNamePrefix, boolean isDaemon, int priority) {
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
