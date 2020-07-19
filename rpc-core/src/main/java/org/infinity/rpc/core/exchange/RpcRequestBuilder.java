package org.infinity.rpc.core.exchange;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.infinity.rpc.core.protocol.constants.ProtocolVersion;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Builder
@Getter
@ToString
public class RpcRequestBuilder implements Requestable, Traceable, Serializable {
    private static final long                serialVersionUID = -6259178379027752471L;
    private              long                requestId;
    private              String              protocol;
    private              byte                protocolVersion  = ProtocolVersion.VERSION_1.getVersion();
    private              String              interfaceName;
    private              String              methodName;
    private              Object[]            methodArguments;
    private              int                 retries          = 0;
    private              Map<String, String> attachments      = new ConcurrentHashMap<>();

    @Override
    public RpcRequestBuilder attachment(String key, String value) {
        attachments.put(key, value);
        return this;
    }

    @Override
    public String getAttachment(String key) {
        return attachments.get(key);
    }

    @Override
    public RpcRequestBuilder sendingTime(long sendingTime) {
        SENDING_TIME.compareAndSet(0, sendingTime);
        return this;
    }

    @Override
    public long getSendingTime() {
        return SENDING_TIME.get();
    }

    @Override
    public RpcRequestBuilder receivedTime(long receivedTime) {
        RECEIVED_TIME.compareAndSet(0, receivedTime);
        return this;
    }

    @Override
    public long getReceivedTime() {
        return RECEIVED_TIME.get();
    }

    @Override
    public RpcRequestBuilder elapsedTime(long elapsedTime) {
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
    public RpcRequestBuilder trace(String key, String value) {
        TRACES.putIfAbsent(key, value);
        return this;
    }

    @Override
    public String getTrace(String key) {
        return TRACES.get(key);
    }
}
