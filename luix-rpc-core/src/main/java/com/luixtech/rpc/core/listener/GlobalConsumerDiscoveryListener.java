package com.luixtech.rpc.core.listener;

import com.luixtech.rpc.core.url.Url;

import java.util.List;

public interface GlobalConsumerDiscoveryListener {
    /**
     * Called by the event which is subscribed.
     *
     * @param registryUrl   registry url
     * @param interfaceName interface name
     * @param consumerUrls  consumer urls
     */
    void onNotify(Url registryUrl, String interfaceName, List<Url> consumerUrls);
}