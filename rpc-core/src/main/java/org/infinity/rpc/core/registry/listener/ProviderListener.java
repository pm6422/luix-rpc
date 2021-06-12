package org.infinity.rpc.core.registry.listener;

import org.infinity.rpc.core.url.Url;

import java.util.List;

/**
 * Provider listener used to discover provider changes event
 */
public interface ProviderListener {

    void onNotify(Url consumerUrl, Url registryUrl, List<Url> providerUrls);
}