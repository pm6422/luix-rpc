package org.infinity.rpc.core.exchange;

import java.util.Map;

public interface Requestable<T> {
    /**
     * Request ID
     *
     * @return
     */
    long getRequestId();

    /**
     * Protocol
     *
     * @return
     */
    String getProtocol();

    /**
     * Protocol version
     *
     * @return
     */
    byte getProtocolVersion();

    /**
     * Provider interface name
     *
     * @return
     */
    String getInterfaceName();

    /**
     * Provider method name
     *
     * @return
     */
    String getMethodName();

    /**
     * Provider method arguments
     *
     * @return
     */
    Object[] getMethodArguments();

    /**
     * Retries count
     *
     * @return
     */
    int getRetries();

    /**
     * Attachments
     *
     * @return
     */
    Map<String, String> getAttachments();

    /**
     * Add attachment
     *
     * @param key
     * @param value
     */
    T attachment(String key, String value);

    /**
     * Get attachment
     *
     * @param key
     * @return
     */
    String getAttachment(String key);
}
