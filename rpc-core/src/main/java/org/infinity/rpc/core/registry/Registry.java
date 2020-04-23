package org.infinity.rpc.core.registry;

public interface Registry extends Registrable, Subscribable {
    Url getUrl();
}
