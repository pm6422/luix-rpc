package org.infinity.rpc.core.exchange.response.impl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.rpc.core.exception.RpcInvocationException;
import org.infinity.rpc.core.exchange.Traceable;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Callbackable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.protocol.constants.ProtocolVersion;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Data
@NoArgsConstructor
@ToString
@Slf4j
public class RpcResponse implements Responseable, Traceable, Callbackable, Serializable {
    private static final long                serialVersionUID = 882479213033600079L;
    private              long                requestId;
    private              String              protocol;
    private              byte                protocolVersion  = ProtocolVersion.VERSION_1.getVersion();
    private              int                 processingTimeout;
    private              Object              result;
    private              Exception           exception;
    private              Map<String, String> attachments      = new ConcurrentHashMap<>();

    @Override
    public String getProtocol() {
        return protocol;
    }

    public RpcResponse protocolVersion(byte protocolVersion) {
        this.protocolVersion = protocolVersion;
        return this;
    }

    @Override
    public byte getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public Map<String, String> getAttachments() {
        return attachments;
    }

    @Override
    public void addAttachment(String key, String value) {
        attachments.putIfAbsent(key, value);
    }

    @Override
    public String getAttachment(String key) {
        return attachments.get(key);
    }

    @Override
    public int getProcessingTimeout() {
        return 0;
    }

    @Override
    public Object getResult() {
        if (exception != null) {
            throw (exception instanceof RuntimeException) ?
                    (RuntimeException) exception :
                    new RpcInvocationException(exception.getMessage(), exception);
        }
        return result;
    }

    @Override
    public Exception getException() {
        return null;
    }

    @Override
    public void setSendingTime(long sendingTime) {
        SENDING_TIME.compareAndSet(0, sendingTime);
    }

    @Override
    public long getSendingTime() {
        return SENDING_TIME.get();
    }

    @Override
    public void setReceivedTime(long receivedTime) {
        RECEIVED_TIME.compareAndSet(0, receivedTime);
    }

    @Override
    public long getReceivedTime() {
        return RECEIVED_TIME.get();
    }

    @Override
    public void setElapsedTime(long elapsedTime) {
        ELAPSED_TIME.compareAndSet(0, elapsedTime);
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
    public void addTrace(String key, String value) {
        TRACES.putIfAbsent(key, value);
    }

    @Override
    public String getTrace(String key) {
        return TRACES.get(key);
    }

    @Override
    public RpcResponse finishCallback(Runnable runnable, Executor executor) {
        if (!FINISHED.get()) {
            TASKS.add(Pair.of(runnable, executor));
        }
        return this;
    }

    @Override
    public void onFinish() {
        if (!FINISHED.compareAndSet(false, true)) {
            return;
        }
        for (Pair<Runnable, Executor> task : TASKS) {
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

    public static RpcResponse error(Requestable request, Exception e) {
        return error(request.getRequestId(), request.getProtocolVersion(), e);
    }

    private static RpcResponse error(long requestId, byte protocolVersion, Exception e) {
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setRequestId(requestId);
        rpcResponse.setProtocolVersion(protocolVersion);
        rpcResponse.setException(e);
        return rpcResponse;
    }
}
