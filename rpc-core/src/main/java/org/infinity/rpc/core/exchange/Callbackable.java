package org.infinity.rpc.core.exchange;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public interface Callbackable<T> {

    AtomicBoolean FINISHED = new AtomicBoolean();

    T finishCallback(Runnable runnable, Executor executor);

    void onFinish();
}
