package org.infinity.rpc.core.exchange;

import java.util.Map;

public interface Exchangable extends Traceable{

    /**
     * @return Request ID
     */
    long getRequestId();

    /**
     * @return Protocol
     */
    String getProtocol();

    /**
     * @return protocol version
     */
    byte getProtocolVersion();

    void setProtocolVersion(byte protocolVersion);

    /**
     * Attachments
     *
     * @return
     */
    Map<String, String> getAttachments();

    /**
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
