package org.infinity.rpc.demoserver.task;

import lombok.Setter;

import java.util.concurrent.ScheduledFuture;

@Setter
public final class ScheduledTask {

    private volatile ScheduledFuture<?> future;

    /**
     * Cancel timed tasks
     */
    public void cancel() {
        ScheduledFuture<?> future = this.future;
        if (future != null) {
            future.cancel(true);
        }
    }
}