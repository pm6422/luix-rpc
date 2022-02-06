package org.infinity.luix.core.registry;

import org.infinity.luix.core.url.Url;

/**
 * Registry interface
 */
public interface Registry extends Registrable, Subscribable {
    /**
     * Get registry type
     *
     * @return registry type
     */
    String getType();

    /**
     * Get registry url
     *
     * @return registry url
     */
    Url getRegistryUrl();
}
