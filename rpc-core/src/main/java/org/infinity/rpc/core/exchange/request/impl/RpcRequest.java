package org.infinity.rpc.core.exchange.request.impl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.protocol.constants.ProtocolVersion;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@ToString
public class RpcRequest implements Requestable, Serializable {
    private static final long                serialVersionUID = -6259178379027752471L;
    private              String              clientRequestId;
    private              long                requestId;
    private              String              protocol;
    private              String              group;
    private              String              version;
    private              byte                protocolVersion  = ProtocolVersion.VERSION_1.getVersion();
    private              String              interfaceName;
    private              String              methodName;
    /**
     * The method parameter type name list string which is separated by comma.
     * e.g, java.util.List,java.lang.Long
     */
    private              String              methodParameters;
    private              Object[]            methodArguments;
    private              Map<String, String> attachments      = new ConcurrentHashMap<>();
    /**
     * Default serialization is hession2
     */
    private              int                 serializeNum     = 0;

    @Override
    public Map<String, String> getAttachments() {
        return attachments;
    }

    @Override
    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
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
    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    @Override
    public void setNumberOfRetry(int retries) {
        RETRIES.compareAndSet(0, retries);
    }

    @Override
    public int getRetries() {
        return RETRIES.get();
    }
}
