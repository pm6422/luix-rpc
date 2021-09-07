package org.infinity.luix.core.registry.listener;

import org.infinity.luix.core.url.Url;

import java.util.List;

/**
 * Provider listener used to discover provider changes event
 */
public interface ProviderListener {

    void onNotify(Url consumerUrl, Url registryUrl, List<Url> providerUrls);
}