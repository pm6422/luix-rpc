package org.infinity.rpc.core.exchange;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.rpc.core.exception.RpcInvocationException;
import org.infinity.rpc.core.protocol.constants.ProtocolVersion;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

@Builder
@Getter
@ToString
@Slf4j
public class RpcResponseBuilder implements Responseable, Traceable, Callbackable, Serializable {
    private static final long serialVersionUID = 882479213033600079L;

    private           long                           requestId;
    private           String                         protocol;
    private           byte                           protocolVersion = ProtocolVersion.VERSION_1.getVersion();
    private           int                            processingTimeout;
    private           Object                         result;
    private           Exception                      exception;
    private           Map<String, String>            attachments     = new ConcurrentHashMap<>();
    private transient List<Pair<Runnable, Executor>> tasks           = new CopyOnWriteArrayList();

    @Override
    public RpcResponseBuilder attachment(String key, String value) {
        attachments.put(key, value);
        return this;
    }

    @Override
    public String getAttachment(String key) {
        return attachments.get(key);
    }

    public Object getResult() {
        if (exception != null) {
            throw (exception instanceof RuntimeException) ?
                    (RuntimeException) exception :
                    new RpcInvocationException(exception.getMessage(), exception);
        }
        return result;
    }

    @Override
    public RpcResponseBuilder sendingTime(long sendingTime) {
        SENDING_TIME.compareAndSet(0, sendingTime);
        return this;
    }

    @Override
    public long getSendingTime() {
        return SENDING_TIME.get();
    }

    @Override
    public RpcResponseBuilder receivedTime(long receivedTime) {
        RECEIVED_TIME.compareAndSet(0, receivedTime);
        return this;
    }

    @Override
    public long getReceivedTime() {
        return RECEIVED_TIME.get();
    }

    @Override
    public RpcResponseBuilder elapsedTime(long elapsedTime) {
        ELAPSED_TIME.compareAndSet(0, elapsedTime);
        return this;
    }

    @Override
    public long getElapsedTime() {
        return getReceivedTime() - getSendingTime();
    }

    @Override
    public Map<String, String> getTraces() {
        return TRACES;
    }

    @Override
    public RpcResponseBuilder trace(String key, String value) {
        TRACES.putIfAbsent(key, value);
        return this;
    }

    @Override
    public String getTrace(String key) {
        return TRACES.get(key);
    }

    @Override
    public RpcResponseBuilder finishCallback(Runnable runnable, Executor executor) {
        if (!FINISHED.get()) {
            tasks.add(Pair.of(runnable, executor));
        }
        return this;
    }

    @Override
    public void onFinish() {
        if (!FINISHED.compareAndSet(false, true)) {
            return;
        }
        for (Pair<Runnable, Executor> task : tasks) {
            Runnable runnable = task.getKey();
            Executor executor = task.getValue();
            if (executor == null) {
                runnable.run();
            } else {
                try {
                    executor.execute(runnable);
                } catch (Exception e) {
                    log.error("Failed to execute the callback with the error: ", e);
                }
            }
        }
    }
}
