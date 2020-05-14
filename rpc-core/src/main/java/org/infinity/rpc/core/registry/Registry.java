package org.infinity.rpc.core.registry;

/**
 * Registry interface
 */
public interface Registry extends Registrable, Subscribable {
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
