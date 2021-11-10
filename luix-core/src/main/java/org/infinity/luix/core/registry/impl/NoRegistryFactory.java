package org.infinity.luix.core.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.registry.AbstractRegistryFactory;
import org.infinity.luix.core.registry.Registry;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.serviceloader.annotation.SpiName;

import static org.infinity.luix.core.constant.RegistryConstants.REGISTRY_VAL_NONE;

@SpiName(REGISTRY_VAL_NONE)
@Slf4j
public class NoRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry createRegistry(Url registryUrl) {
        return new NoRegistry(registryUrl);
    }
}
