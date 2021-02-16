package org.infinity.rpc.core.client.request;

import org.infinity.rpc.core.exchange.Exchangable;

public interface Requestable extends Exchangable {

    /**
     * Get provider interface name
     *
     * @return provider interface name
     */
    String getInterfaceName();

    /**
     * Get method name
     *
     * @return method name
     */
    String getMethodName();

    /**
     * Get the method parameter class name list string which is separated by comma.
     * e.g, java.util.List,java.lang.Long
     *
     * @return method parameter class name list string
     */
    String getMethodParameters();

    /**
     * Get method arguments
     *
     * @return method arguments
     */
    Object[] getMethodArguments();

    /**
     * Set the number of RPC request retry
     *
     * @param numberOfRetry number of retry
     */
    void setRetryNumber(int numberOfRetry);

    /**
     * Get the number of RPC request retry
     *
     * @return number of retry
     */
    int getRetryNumber();

    /**
     * set the serialization number.
     * same to the protocol version, this value only used in server end for compatible.
     *
     * @param number number
     */
    void setSerializeNum(int number);

    /**
     * Get serialize number
     *
     * @return serialize number
     */
    int getSerializeNum();
}
