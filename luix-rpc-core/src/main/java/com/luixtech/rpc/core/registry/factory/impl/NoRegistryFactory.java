package com.luixtech.rpc.core.registry.factory.impl;

import com.luixtech.rpc.core.registry.Registry;
import com.luixtech.rpc.core.registry.factory.AbstractRegistryFactory;
import com.luixtech.rpc.core.registry.impl.NoRegistry;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.utilities.serviceloader.annotation.SpiName;
import lombok.extern.slf4j.Slf4j;

import static com.luixtech.rpc.core.constant.RegistryConstants.REGISTRY_VAL_NONE;

@SpiName(REGISTRY_VAL_NONE)
@Slf4j
public class NoRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry createRegistry(Url registryUrl) {
        return new NoRegistry(registryUrl);
    }
}
