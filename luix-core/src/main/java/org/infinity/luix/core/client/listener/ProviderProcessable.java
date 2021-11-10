package org.infinity.luix.core.client.listener;

import org.infinity.luix.core.url.Url;

import java.util.List;

public interface ProviderProcessable {
    /**
     * Process providers
     *
     * @param registryUrl   registry url
     * @param interfaceName interface name
     * @param providerUrls  provider urls
     */
    void process(Url registryUrl, String interfaceName, List<Url> providerUrls);
}
