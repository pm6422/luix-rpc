package org.infinity.rpc.core.exchange.request;

import org.infinity.rpc.core.exchange.Exchangable;

import java.util.concurrent.atomic.AtomicInteger;

public interface Requestable extends Exchangable {

    AtomicInteger RETRIES = new AtomicInteger(0);

    String getClientRequestId();

    void setClientRequestId(String clientRequestId);

    // Provider interface name
    String getInterfaceName();

    // Provider method name
    String getMethodName();

    // Provider method arguments
    Object[] getMethodArguments();

    // Set the number of RPC request retry
    void setNumberOfRetry(int numberOfRetry);

    int getRetries();

    void setProtocol(String protocol);
}
