package org.infinity.rpc.core.server.listener;

import org.infinity.rpc.core.url.Url;

import java.util.List;

public interface ConsumerProcessable {
    /**
     * Process consumers
     *
     * @param registryUrl   registry url
     * @param consumerUrls  consumer urls
     * @param interfaceName interface name
     */
    void process(Url registryUrl, List<Url> consumerUrls, String interfaceName);
}