package org.infinity.rpc.core.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.registry.AbstractRegistryFactory;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.annotation.NamedAs;

@NamedAs("direct")
@Slf4j
public class DirectRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry createRegistry(Url registryUrl) {
        return new DirectRegistry(registryUrl);
    }
}
