package org.infinity.rpc.core.exchange.response.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.Traceable;
import org.infinity.rpc.core.exchange.TraceableContext;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.FutureListener;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.RpcResponseFuture;
import org.infinity.rpc.core.exchange.serialization.DeserializableObject;
import org.infinity.rpc.core.exchange.transport.constants.FutureState;
import org.infinity.rpc.core.protocol.constants.ProtocolVersion;
import org.infinity.rpc.core.url.Url;

import java.io.IOException;
import java.util.*;

@Slf4j
public class DefaultResponseFuture implements RpcResponseFuture {

    protected final    Object               lock             = new Object();
    protected volatile FutureState          state            = FutureState.DOING;
    protected          Object               result           = null;
    protected          Exception            exception        = null;
    protected          long                 createTime       = System.currentTimeMillis();
    protected          int                  timeout          = 0;
    protected          long                 processTime      = 0;
    protected          Requestable          request;
    protected          List<FutureListener> listeners;
    protected          Url                  serverUrl;
    protected          Class                returnType;
    private            Map<String, String>  attachments;// rpc协议版本兼容时可以回传一些额外的信息
    private            TraceableContext     traceableContext = new TraceableContext();

    public DefaultResponseFuture(Requestable requestObj, int timeout, Url serverUrl) {
        this.request = requestObj;
        this.timeout = timeout;
        this.serverUrl = serverUrl;
    }

    @Override
    public void onSuccess(Responseable response) {
        this.result = response.getResult();
        this.processTime = response.getElapsedTime();
        this.attachments = response.getAttachments();
        if (response instanceof Traceable) {
            traceableContext.setReceiveTime(response.getReceivedTime());
            response.getTraces().entrySet().forEach(trace -> {
                traceableContext.addTraceInfo(trace.getKey(), trace.getValue());
            });
        }

        done();
    }

    @Override
    public void onFailure(Responseable response) {
        this.exception = response.getException();
        this.processTime = response.getElapsedTime();
        done();
    }

    @Override
    public Object getValue() {
        synchronized (lock) {
            if (!isDoing()) {
                return getValueOrThrowable();
            }

            if (timeout <= 0) {
                try {
                    lock.wait();
                } catch (Exception e) {
                    cancel(new RpcServiceException(this.getClass().getName() + " getValue InterruptedException : "
                            + request.toString() + " cost=" + (System.currentTimeMillis() - createTime), e));
                }

                return getValueOrThrowable();
            } else {
                long waitTime = timeout - (System.currentTimeMillis() - createTime);

                if (waitTime > 0) {
                    for (; ; ) {
                        try {
                            lock.wait(waitTime);
                        } catch (InterruptedException e) {
                        }

                        if (!isDoing()) {
                            break;
                        } else {
                            waitTime = timeout - (System.currentTimeMillis() - createTime);
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
            return getValueOrThrowable();
        }
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public boolean cancel() {
        Exception e = new RpcServiceException(this.getClass().getName() + " task cancel: serverPort=" + serverUrl.getServerPortStr() + " "
                        + request.toString() + " cost=" + (System.currentTimeMillis() - createTime));
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
    public long getCreateTime() {
        return createTime;
    }

    @Override
    public void setReturnType(Class<?> clazz) {
        this.returnType = clazz;
    }

    public Object getRequestObj() {
        return request;
    }

    public FutureState getState() {
        return state;
    }

    private void timeoutSoCancel() {
        this.processTime = System.currentTimeMillis() - createTime;

        synchronized (lock) {
            if (!isDoing()) {
                return;
            }

            state = FutureState.CANCELLED;
            exception = new RpcServiceException(this.getClass().getName() + " request timeout: serverPort=" + serverUrl.getServerPortStr()
                            + " " + request + " cost=" + (System.currentTimeMillis() - createTime),
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
    public String getProtocol() {
        return null;
    }

    @Override
    public byte getProtocolVersion() {
        return 0;
    }

    private Object getValueOrThrowable() {
        if (exception != null) {
            throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new RpcServiceException(
                    exception.getMessage(), exception);
        }
        if (result != null && returnType != null && result instanceof DeserializableObject) {
            try {
                result = ((DeserializableObject) result).deserialize(returnType);
            } catch (IOException e) {
                log.error("deserialize response value fail! return type:" + returnType, e);
                throw new RpcFrameworkException("deserialize return value fail! deserialize type:" + returnType, e);
            }
        }
        return result;
    }

    @Override
    public long getElapsedTime() {
        return processTime;
    }

    @Override
    public Map<String, String> getTraces() {
        return null;
    }

    @Override
    public void addTrace(String key, String value) {

    }

    @Override
    public String getTrace(String key) {
        return null;
    }

    @Override
    public void setSendingTime(long sendingTime) {

    }

    @Override
    public long getSendingTime() {
        return 0;
    }

    @Override
    public void setReceivedTime(long receivedTime) {

    }

    @Override
    public long getReceivedTime() {
        return 0;
    }

    @Override
    public void setElapsedTime(long time) {
        this.processTime = time;
    }

    @Override
    public Object getResult() {
        return null;
    }

    @Override
    public int getProcessingTimeout() {
        return timeout;
    }

    @Override
    public Map<String, String> getAttachments() {
        return attachments != null ? attachments : Collections.<String, String>emptyMap();
    }

    @Override
    public void setAttachments(Map<String, String> attachments) {

    }

    @Override
    public void addAttachment(String key, String value) {
        if (this.attachments == null) {
            this.attachments = new HashMap<>();
        }
        this.attachments.put(key, value);
    }

    @Override
    public String getAttachment(String key) {
        return null;
    }


    @Override
    public void setSerializeNumber(int number) {
    }

    @Override
    public int getSerializeNumber() {
        return 0;
    }
}
