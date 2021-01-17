package org.infinity.rpc.core.exchange;

import java.util.Map;

public interface Exchangable extends Traceable {

    /**
     * Get request ID
     *
     * @return Request ID
     */
    long getRequestId();

    /**
     * Get protocol version
     *
     * @return protocol version
     */
    byte getProtocolVersion();

    /**
     * Set protocol version
     *
     * @param protocolVersion protocol version
     */
    void setProtocolVersion(byte protocolVersion);

    /**
     * Get request options
     *
     * @return request options
     */
    Map<String, String> getOptions();

    /**
     * Set request options
     *
     * @param options request options
     */
    void setOptions(Map<String, String> options);

    /**
     * Add request option
     *
     * @param key   request option key
     * @param value request option value
     */
    void addOption(String key, String value);

    /**
     * Get request option
     *
     * @param key request option key
     * @return request option
     */
    String getOption(String key);
}
