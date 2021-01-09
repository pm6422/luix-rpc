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

    /**
     * Get the method parameter class name list string which is separated by comma.
     * e.g, java.util.List,java.lang.Long
     *
     * @return method parameter class name list string
     */
    String getParameterTypeList();

    // Provider method arguments
    Object[] getMethodArguments();

    // Set the number of RPC request retry
    void setNumberOfRetry(int numberOfRetry);

    int getRetries();

    void setProtocol(String protocol);

    /**
     * set the serialization number.
     * same to the protocol version, this value only used in server end for compatible.
     *
     * @param number
     */
    void setSerializeNum(int number);

    int getSerializeNum();
}
