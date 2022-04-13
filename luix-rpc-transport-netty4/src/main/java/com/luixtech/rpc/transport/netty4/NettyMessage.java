package com.luixtech.rpc.transport.netty4;


import com.luixtech.rpc.core.protocol.constants.ProtocolVersion;

public class NettyMessage {
    private boolean         isRequest;
    private long            requestId;
    private byte[]          data;
    private long            startTime;
    private ProtocolVersion version;

    public NettyMessage(boolean isRequest, long requestId, byte[] data, ProtocolVersion version) {
        this.isRequest = isRequest;
        this.requestId = requestId;
        this.data = data;
        this.version = version;
    }

    public boolean isRequest() {
        return isRequest;
    }

    public void setRequest(boolean request) {
        isRequest = request;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public ProtocolVersion getVersion() {
        return version;
    }

    public void setVersion(ProtocolVersion version) {
        this.version = version;
    }
}
