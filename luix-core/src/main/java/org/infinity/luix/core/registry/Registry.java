package org.infinity.luix.core.registry;

/**
 * Registry interface
 */
public interface Registry extends Registrable, Subscribable {
    /**
     * Get registry type name
     *
     * @return registry type name
     */
    String getName();
}
