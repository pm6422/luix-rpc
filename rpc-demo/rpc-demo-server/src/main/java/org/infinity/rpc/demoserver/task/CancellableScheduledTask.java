package org.infinity.rpc.demoserver.task;

import lombok.AllArgsConstructor;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

@AllArgsConstructor
public final class CancellableScheduledTask {

    private ScheduledFuture<?> future;

    /**
     * Cancel timed tasks
     */
    public void cancel() {
        Optional.ofNullable(future).ifPresent(future -> future.cancel(true));
    }
}