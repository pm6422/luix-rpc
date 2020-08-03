package org.infinity.rpc.core.exchange.request.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.infinity.rpc.core.exchange.Traceable;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.protocol.constants.ProtocolVersion;

import java.io.Serializable;
import java.util.Map;

@Builder
@Getter
@ToString
public class RpcRequest implements Requestable<RpcRequest>, Traceable<RpcRequest>, Serializable {
    private static final long     serialVersionUID = -6259178379027752471L;
    private              String   clientRequestId;
    private final        long     requestId;
    private final        String   protocol;
    private final        byte     protocolVersion  = ProtocolVersion.VERSION_1.getVersion();
    private final        String   interfaceName;
    private final        String   methodName;
    private final        String[] parameterTypeNames;
    private final        Object[] methodArguments;

    @Override
    public Map<String, String> getAttachments() {
        return ATTACHMENTS;
    }

    @Override
    public RpcRequest attachment(String key, String value) {
        ATTACHMENTS.putIfAbsent(key, value);
        return this;
    }

    @Override
    public String getAttachment(String key) {
        return ATTACHMENTS.get(key);
    }

    @Override
    public RpcRequest sendingTime(long sendingTime) {
        SENDING_TIME.compareAndSet(0, sendingTime);
        return this;
    }

    @Override
    public long getSendingTime() {
        return SENDING_TIME.get();
    }

    @Override
    public RpcRequest receivedTime(long receivedTime) {
        RECEIVED_TIME.compareAndSet(0, receivedTime);
        return this;
    }

    @Override
    public long getReceivedTime() {
        return RECEIVED_TIME.get();
    }

    @Override
    public RpcRequest elapsedTime(long elapsedTime) {
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
    public RpcRequest trace(String key, String value) {
        TRACES.putIfAbsent(key, value);
        return this;
    }

    @Override
    public String getTrace(String key) {
        return TRACES.get(key);
    }

    @Override
    public RpcRequest clientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
        return this;
    }

    @Override
    public RpcRequest retries(int retries) {
        RETRIES.compareAndSet(0, retries);
        return this;
    }

    @Override
    public int getRetries() {
        return RETRIES.get();
    }
}
