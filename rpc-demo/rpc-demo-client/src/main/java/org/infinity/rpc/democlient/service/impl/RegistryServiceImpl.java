package org.infinity.rpc.democlient.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.democlient.dto.RegistryDTO;
import org.infinity.rpc.democlient.service.RegistryService;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RegistryServiceImpl implements RegistryService, InitializingBean {
    private static final Map<String, Registry> REGISTRY_MAP = new ConcurrentHashMap<>();
    private static final List<RegistryDTO>     REGISTRIES   = new ArrayList<>();

    @Resource
    private       InfinityProperties  infinityProperties;
    private final ProviderProcessable providerProcessService;

    public RegistryServiceImpl(ProviderProcessable providerProcessService) {
        this.providerProcessService = providerProcessService;
    }

    @Override
    public void afterPropertiesSet() {
        if (CollectionUtils.isEmpty(infinityProperties.getRegistryList())) {
            log.warn("No registries found!");
            return;
        }
        infinityProperties.getRegistryList().forEach(registryConfig -> {
            REGISTRY_MAP.put(registryConfig.getRegistryUrl().getIdentity(), registryConfig.getRegistryImpl());
            log.info("Found registry: [{}]", registryConfig.getRegistryUrl().getIdentity());
            REGISTRIES.add(new RegistryDTO(registryConfig.getRegistryImpl().getType(), registryConfig.getRegistryUrl().getIdentity()));

            registryConfig.getRegistryImpl().getAllProviderPaths().forEach(interfaceName -> createConsumerStub(interfaceName, registryConfig));
        });
    }

    private void createConsumerStub(String interfaceName, RegistryConfig registryConfig) {
        ConsumerStub.create(interfaceName, infinityProperties.getApplication(), registryConfig,
                infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                providerProcessService, null, null, null, null);
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
