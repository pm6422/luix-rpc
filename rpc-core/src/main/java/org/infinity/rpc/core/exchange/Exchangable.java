package org.infinity.rpc.core.exchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface Exchangable {
    Map<String, String> ATTACHMENTS = new ConcurrentHashMap<>();

    /**
     * Request ID
     *
     * @return
     */
    long getRequestId();

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
    void addAttachment(String key, String value);

    /**
     * Get attachment
     *
     * @param key
     * @return
     */
    String getAttachment(String key);
}
