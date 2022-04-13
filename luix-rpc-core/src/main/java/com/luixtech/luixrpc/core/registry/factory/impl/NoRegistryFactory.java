package com.luixtech.luixrpc.core.registry.factory.impl;

import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.registry.factory.AbstractRegistryFactory;
import com.luixtech.luixrpc.core.registry.Registry;
import com.luixtech.luixrpc.core.registry.impl.NoRegistry;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.utilities.serviceloader.annotation.SpiName;

import static com.luixtech.luixrpc.core.constant.RegistryConstants.REGISTRY_VAL_NONE;

@SpiName(REGISTRY_VAL_NONE)
@Slf4j
public class NoRegistryFactory extends AbstractRegistryFactory {
    @Override
    public Registry createRegistry(Url registryUrl) {
        return new NoRegistry(registryUrl);
    }
}
