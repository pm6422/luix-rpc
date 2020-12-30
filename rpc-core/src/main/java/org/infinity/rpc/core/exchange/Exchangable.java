package org.infinity.rpc.core.exchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface Exchangable {

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
     *
     * @param attachments
     */
    void setAttachments(Map<String, String> attachments);

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
