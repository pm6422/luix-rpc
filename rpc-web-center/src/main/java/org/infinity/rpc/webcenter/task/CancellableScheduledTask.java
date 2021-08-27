package org.infinity.rpc.webcenter.task;

import java.util.concurrent.ScheduledFuture;

public final class CancellableScheduledTask {

    volatile ScheduledFuture<?> future;

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