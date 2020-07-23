package org.infinity.rpc.core.exchange.response;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public interface Callbackable<T> {
    List<Pair<Runnable, Executor>> TASKS = new CopyOnWriteArrayList();

    AtomicBoolean FINISHED = new AtomicBoolean();

    T finishCallback(Runnable runnable, Executor executor);

    void onFinish();
}
