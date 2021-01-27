package org.infinity.rpc.demoserver.service.impl;

import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.demoserver.dto.RegistryDTO;
import org.infinity.rpc.demoserver.service.RegistryService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegistryServiceImpl implements RegistryService, InitializingBean {
    private static final Map<String, Registry> REGISTRY_MAP = new ConcurrentHashMap<>();
    private static final List<RegistryDTO>     REGISTRIES   = new ArrayList<>();

    private final InfinityProperties infinityProperties;

    public RegistryServiceImpl(InfinityProperties infinityProperties) {
        this.infinityProperties = infinityProperties;
    }

    @Override
    public void afterPropertiesSet() {
        infinityProperties.getRegistryConfigs().forEach(registryConfig -> {
            REGISTRY_MAP.put(registryConfig.getRegistryUrl().getIdentity(), registryConfig.getRegistryImpl());
            REGISTRIES.add(new RegistryDTO(registryConfig.getRegistryImpl().getType(), registryConfig.getRegistryUrl().getIdentity()));
        });
    }

    @Override
    public List<RegistryDTO> getRegistries() {
        return REGISTRIES;
    }

    @Override
    public Registry findRegistry(String urlIdentity) {
        return REGISTRY_MAP.get(urlIdentity);
    }
}
