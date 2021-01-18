package org.infinity.rpc.core.exchange.response.impl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.rpc.core.exception.RpcInvocationException;
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
public class RpcResponse implements Responseable, Callbackable, Serializable {
    private static final long                serialVersionUID = 882479213033600079L;
    protected            long                requestId;
    protected            byte                protocolVersion  = ProtocolVersion.VERSION_1.getVersion();
    protected            String              group;
    protected            String              version;
    protected            int                 timeout;
    protected            Object              result;
    protected            Exception           exception;
    protected            long                sendingTime;
    protected            long                receivedTime;
    protected            long                elapsedTime;
    protected            Map<String, String> traces           = new ConcurrentHashMap<>();
    /**
     * RPC request options, all the optional RPC request parameters will be put in it.
     */
    protected            Map<String, String> options          = new ConcurrentHashMap<>();
    /**
     * default serialization is hession2
     */
    protected            int                 serializeNum     = 0;

    public RpcResponse(Object result) {
        this.result = result;
    }

    public RpcResponse(long requestId, Object result) {
        this.requestId = requestId;
        this.result = result;
    }

    public RpcResponse(Responseable response) {
        this.result = response.getResult();
        this.exception = response.getException();
        this.requestId = response.getRequestId();
        this.setElapsedTime(response.getElapsedTime());
        this.timeout = response.getTimeout();
        this.protocolVersion = response.getProtocolVersion();
        this.setSerializeNum(response.getSerializeNum());
        this.options = response.getOptions();
        this.setReceivedTime(response.getReceivedTime());
        response.getTraces().forEach((key, value) -> this.addTrace(key, key));
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
    public void addOption(String key, String value) {
        options.putIfAbsent(key, value);
    }

    @Override
    public String getOption(String key) {
        return options.get(key);
    }

    @Override
    public void addTrace(String key, String value) {
        traces.putIfAbsent(key, value);
    }

    @Override
    public String getTrace(String key) {
        return traces.get(key);
    }

    @Override
    public void addFinishCallback(Runnable runnable, Executor executor) {
        if (!FINISHED.get()) {
            TASKS.add(Pair.of(runnable, executor));
        }
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
