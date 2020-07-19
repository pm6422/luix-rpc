package org.infinity.rpc.core.exchange;

import java.util.Map;

public interface Exchangable<T> {
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
