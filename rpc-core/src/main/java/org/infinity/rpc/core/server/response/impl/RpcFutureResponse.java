package org.infinity.rpc.core.server.response.impl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.TraceableContext;
import org.infinity.rpc.core.exchange.constants.FutureState;
import org.infinity.rpc.core.protocol.constants.ProtocolVersion;
import org.infinity.rpc.core.serialization.DeserializableObject;
import org.infinity.rpc.core.server.response.FutureListener;
import org.infinity.rpc.core.server.response.FutureResponse;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.url.Url;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@ToString
public class RpcFutureResponse implements FutureResponse, Serializable {
    private static final long                 serialVersionUID = -8089955194208179445L;
    protected final      Object               lock             = new Object();
    protected volatile   FutureState          state            = FutureState.DOING;
    protected            Object               resultObject;
    protected            Exception            exception;
    protected            long                 createdTime      = System.currentTimeMillis();
    protected            byte                 protocolVersion  = ProtocolVersion.VERSION_1.getVersion();
    protected            String               group;
    protected            String               version;
    protected            int                  timeout;
    protected            long                 processTime      = 0;
    protected            Requestable          request;
    protected            List<FutureListener> listeners;
    protected            Url                  serverUrl;
    protected            Class<?>             returnType;
    protected            long                 sendingTime;
    protected            long                 receivedTime;
    protected            long                 elapsedTime;
    protected            Map<String, String>  traces           = new ConcurrentHashMap<>();
    /**
     * RPC request options, all the optional RPC request parameters will be put in it.
     */
    protected            Map<String, String>  options          = new ConcurrentHashMap<>();
    protected            TraceableContext     traceableContext = new TraceableContext();
    /**
     * default serialization is hession2
     */
    protected            int                  serializeNum     = 0;

    public RpcFutureResponse(Requestable requestObj, int timeout, Url serverUrl) {
        this.request = requestObj;
        this.timeout = timeout;
        this.serverUrl = serverUrl;
    }

    @Override
    public void onSuccess(Responseable response) {
        this.resultObject = response.getResult();
        this.processTime = response.getElapsedTime();
        this.options = response.getOptions();
        traceableContext.setReceiveTime(response.getReceivedTime());
        response.getTraces().forEach((key, value) -> traceableContext.addTraceInfo(key, value));
        done();
    }

    @Override
    public void onFailure(Responseable response) {
        this.exception = response.getException();
        this.processTime = response.getElapsedTime();
        done();
    }

    @Override
    public boolean cancel() {
        Exception e = new RpcServiceException(this.getClass().getName() + " task cancel: serverPort=" + serverUrl.getAddress() + " "
                + request.toString() + " cost=" + (System.currentTimeMillis() - createdTime));
        return cancel(e);
    }

    protected boolean cancel(Exception e) {
        synchronized (lock) {
            if (!isDoing()) {
                return false;
            }

            state = FutureState.CANCELLED;
            exception = e;
            lock.notifyAll();
        }

        notifyListeners();
        return true;
    }

    @Override
    public boolean isCancelled() {
        return state.isCancelledState();
    }

    @Override
    public boolean isDone() {
        return state.isDoneState();
    }

    @Override
    public boolean isSuccess() {
        return isDone() && (exception == null);
    }

    @Override
    public void addListener(FutureListener listener) {
        if (listener == null) {
            throw new NullPointerException("FutureListener is null");
        }

        boolean notifyNow = false;
        synchronized (lock) {
            if (!isDoing()) {
                notifyNow = true;
            } else {
                if (listeners == null) {
                    listeners = new ArrayList<>(1);
                }

                listeners.add(listener);
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
    }

    @Override
    public void setReturnType(Class<?> clazz) {
        this.returnType = clazz;
    }

    public FutureState getState() {
        return state;
    }

    private void timeoutSoCancel() {
        this.processTime = System.currentTimeMillis() - createdTime;

        synchronized (lock) {
            if (!isDoing()) {
                return;
            }

            state = FutureState.CANCELLED;
            exception = new RpcServiceException(this.getClass().getName() + " request timeout: serverPort=" + serverUrl.getAddress()
                    + " " + request + " cost=" + (System.currentTimeMillis() - createdTime),
                    RpcErrorMsgConstant.SERVICE_TIMEOUT);

            lock.notifyAll();
        }

        notifyListeners();
    }

    private void notifyListeners() {
        if (listeners != null) {
            for (FutureListener listener : listeners) {
                notifyListener(listener);
            }
        }
    }

    private void notifyListener(FutureListener listener) {
        try {
            listener.operationComplete(this);
        } catch (Throwable t) {
            log.error(this.getClass().getName() + " notifyListener Error: " + listener.getClass().getSimpleName(), t);
        }
    }

    private boolean isDoing() {
        return state.isDoingState();
    }

    protected boolean done() {
        synchronized (lock) {
            if (!isDoing()) {
                return false;
            }

            state = FutureState.DONE;
            lock.notifyAll();
        }

        notifyListeners();
        return true;
    }

    @Override
    public long getRequestId() {
        return this.request.getRequestId();
    }

    @Override
    public Object getResult() {
        synchronized (lock) {
            if (!isDoing()) {
                return getResultOrThrowable();
            }

            if (timeout <= 0) {
                try {
                    lock.wait();
                } catch (Exception e) {
                    cancel(new RpcServiceException(this.getClass().getName() + " getValue InterruptedException : "
                            + request.toString() + " cost=" + (System.currentTimeMillis() - createdTime), e));
                }
                return getResultOrThrowable();
            } else {
                long waitTime = timeout - (System.currentTimeMillis() - createdTime);
                if (waitTime > 0) {
                    for (; ; ) {
                        try {
                            lock.wait(waitTime);
                        } catch (InterruptedException e) {
                            // Leave blank intentionally
                        }

                        if (!isDoing()) {
                            break;
                        } else {
                            waitTime = timeout - (System.currentTimeMillis() - createdTime);
                            if (waitTime <= 0) {
                                break;
                            }
                        }
                    }
                }

                if (isDoing()) {
                    timeoutSoCancel();
                }
            }
            return getResultOrThrowable();
        }
    }

    private Object getResultOrThrowable() {
        if (exception != null) {
            throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new RpcServiceException(
                    exception.getMessage(), exception);
        }
        if (resultObject != null && returnType != null && resultObject instanceof DeserializableObject) {
            try {
                resultObject = ((DeserializableObject) resultObject).deserialize(returnType);
            } catch (IOException e) {
                log.error("deserialize response value fail! return type:" + returnType, e);
                throw new RpcFrameworkException("deserialize return value fail! deserialize type:" + returnType, e);
            }
        }
        return resultObject;
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
    public void addOption(String key, String value) {
        if (this.options == null) {
            this.options = new HashMap<>(10);
        }
        this.options.put(key, value);
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
}
