package org.infinity.luix.core.server.response.impl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.exception.impl.RpcInvocationException;
import org.infinity.luix.core.protocol.constants.ProtocolVersion;
import org.infinity.luix.core.server.response.Callbackable;
import org.infinity.luix.core.server.response.Responseable;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class RpcResponse implements Responseable, Callbackable, Serializable {
    private static final long                serialVersionUID = 882479213033600079L;
    protected            long                requestId;
    protected            String              protocol;
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
     *
     */
    protected            int                 serializerId;

    public static RpcResponse of(Responseable resp) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(resp.getRequestId());
        response.setResult(resp.getResult());
        response.setException(resp.getException());
        response.setReceivedTime(resp.getReceivedTime());
        response.setElapsedTime(resp.getElapsedTime());
        response.setTimeout(resp.getTimeout());
        response.setProtocolVersion(resp.getProtocolVersion());
        response.setSerializerId(resp.getSerializerId());
        response.setOptions(resp.getOptions());
        resp.getTraces().forEach((key, value) -> response.addTrace(key, key));
        return response;
    }

    public static RpcResponse of(Object result) {
        RpcResponse response = new RpcResponse();
        response.setResult(result);
        return response;
    }

    public RpcResponse of(long requestId, Object result) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setResult(result);
        return response;
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
    public String getOption(String key, String defaultValue) {
        String value = getOption(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public int getIntOption(String key) {
        return Integer.parseInt(options.get(key));
    }

    @Override
    public int getIntOption(String key, int defaultValue) {
        String value = getOption(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
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
}
