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
     * Set options
     *
     * @param options options
     */
    void setOptions(Map<String, String> options);

    /**
     * Add option
     *
     * @param key   option key
     * @param value option value
     */
    void addOption(String key, String value);

    /**
     * Get option value
     *
     * @param key option key
     * @return option value
     */
    String getOption(String key);

    /**
     * Get option value in integer form
     *
     * @param key option key
     * @return option value
     */
    int getIntOption(String key);
}
