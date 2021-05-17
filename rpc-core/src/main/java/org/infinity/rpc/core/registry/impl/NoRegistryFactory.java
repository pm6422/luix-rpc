package org.infinity.rpc.core.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.registry.AbstractRegistryFactory;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import static org.infinity.rpc.core.constant.RegistryConstants.REGISTRY_VAL_NONE;

@SpiName(REGISTRY_VAL_NONE)
@Slf4j
public class NoRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry createRegistry(Url registryUrl) {
        return new NoRegistry(registryUrl);
    }
}
