package org.infinity.luix.core.registry;

import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.subscribe.Subscribable;

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
     * Get registry subclass name
     *
     * @return registry subclass name
     */
    String getRegistryClassName();

    /**
     * Get registry url
     *
     * @return registry url
     */
    Url getRegistryUrl();
}
