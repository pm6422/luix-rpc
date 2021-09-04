package org.infinity.luix.core.server.listener;

import org.infinity.luix.core.url.Url;

import java.util.List;

public interface ConsumerProcessable {
    /**
     * Process consumers
     *
     * @param registryUrl   registry url
     * @param interfaceName interface name
     * @param consumerUrls  consumer urls
     */
    void process(Url registryUrl, String interfaceName, List<Url> consumerUrls);
}