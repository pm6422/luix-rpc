package org.infinity.rpc.core.registry;

/**
 * Registry interface
 */
public interface Registry extends Registrable, Subscribable {
    String getRegistryClassName();

    Url getRegistryUrl();
}
