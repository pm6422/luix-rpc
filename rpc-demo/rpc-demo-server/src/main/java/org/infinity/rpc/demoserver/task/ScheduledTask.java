package org.infinity.rpc.demoserver.task;

import lombok.AllArgsConstructor;

import java.util.concurrent.ScheduledFuture;

@AllArgsConstructor
public final class ScheduledTask {

    private ScheduledFuture<?> future;

    /**
     * Cancel timed tasks
     */
    public void cancel() {
        if (future != null) {
            future.cancel(true);
        }
    }
}