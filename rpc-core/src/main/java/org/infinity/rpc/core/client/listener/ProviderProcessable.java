package org.infinity.rpc.core.client.listener;

import org.infinity.rpc.core.url.Url;

import java.util.List;

public interface ProviderProcessable {
    /**
     * Process providers
     *
     * @param registryUrl   registry url
     * @param providerUrls  provider urls
     * @param interfaceName interface name
     */
    void process(Url registryUrl, List<Url> providerUrls, String interfaceName);
}
