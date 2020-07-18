package org.infinity.rpc.core.exchange;

import java.io.Serializable;

public class RpcResponse implements Responseable, Serializable {
    private static final long serialVersionUID = 882479213033600079L;

    @Override
    public long getRequestId() {
        return 0;
    }

    @Override
    public Object getResult() {
        return null;
    }

    @Override
    public Exception getException() {
        return null;
    }

    @Override
    public void setElapsedTime() {

    }

    @Override
    public long getElapsedTime() {
        return 0;
    }

    @Override
    public int getProcessingTimeout() {
        return 0;
    }

    @Override
    public String getProtocolVersion() {
        return null;
    }
}
