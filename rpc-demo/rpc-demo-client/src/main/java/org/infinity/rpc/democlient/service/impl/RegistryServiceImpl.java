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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.constant.ServiceConstants.FORM;
import static org.infinity.rpc.core.constant.ServiceConstants.VERSION;

@Service
@Slf4j
public class RegistryServiceImpl implements RegistryService, InitializingBean {
    private static final Map<String, Registry> REGISTRY_MAP = new ConcurrentHashMap<>();
    private static final List<RegistryDTO>     REGISTRIES   = new ArrayList<>();

    private final InfinityProperties  infinityProperties;
    private final ProviderProcessable providerProcessService;

    public RegistryServiceImpl(InfinityProperties infinityProperties, ProviderProcessable providerProcessService) {
        this.infinityProperties = infinityProperties;
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

    private ConsumerStub<?> createConsumerStub(String interfaceName, RegistryConfig registryConfig) {
        ConsumerStub<?> consumerStub = new ConsumerStub<>();
        consumerStub.setInterfaceName(interfaceName);
        consumerStub.setProtocol(infinityProperties.getAvailableProtocol().getName());
        consumerStub.setCluster(infinityProperties.getConsumer().getCluster());
        consumerStub.setFaultTolerance(infinityProperties.getConsumer().getFaultTolerance());
        consumerStub.setLoadBalancer(infinityProperties.getConsumer().getLoadBalancer());
        consumerStub.setProxy(infinityProperties.getConsumer().getProxyFactory());
        consumerStub.setHealthChecker(infinityProperties.getConsumer().getHealthChecker());

//        consumerStub.setForm(dto.getOptions().get(FORM));
//        consumerStub.setVersion(dto.getOptions().get(VERSION));
        // Must NOT call init()

        consumerStub.subscribeProviders(infinityProperties.getApplication(), infinityProperties.getAvailableProtocol(),
                registryConfig, providerProcessService);
        return consumerStub;
    }

    @Override
    public List<RegistryDTO> getRegistries() {
        return REGISTRIES;
    }

    @Override
    public Registry findRegistry(String urlIdentity) {
        return REGISTRY_MAP.get(urlIdentity);
    }

    @Override
    public Object getAllApps() {
        return null;
    }
}
