package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.listener.ProviderProcessable;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.client.stub.ConsumerStubHolder;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.server.listener.ConsumerProcessable;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.webcenter.dto.RpcRegistryDTO;
import org.infinity.rpc.webcenter.service.RpcRegistryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.constant.ServiceConstants.FORM;
import static org.infinity.rpc.core.constant.ServiceConstants.VERSION;

@Service
@Slf4j
public class RpcRegistryServiceImpl implements RpcRegistryService, ApplicationRunner {
    private static final Map<String, RegistryConfig> REGISTRY_CONFIG_MAP = new ConcurrentHashMap<>();
    private static final List<RpcRegistryDTO>        REGISTRIES          = new ArrayList<>();
    @Resource
    private              InfinityProperties          infinityProperties;
    @Resource
    private              ProviderProcessable         providerProcessService;
    @Resource
    private              ConsumerProcessable         consumerProcessService;

    /**
     * {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet()} execute too earlier
     *
     * @param args arguments
     * @throws Exception if any exception throws
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (CollectionUtils.isEmpty(infinityProperties.getRegistryList())) {
            log.warn("No registries found!");
            return;
        }
        try {
            infinityProperties.getRegistryList().forEach(registryConfig -> {
                REGISTRY_CONFIG_MAP.put(registryConfig.getRegistryUrl().getIdentity(), registryConfig);
                REGISTRIES.add(new RpcRegistryDTO(registryConfig.getRegistryImpl().getType(), registryConfig.getRegistryUrl().getIdentity()));
                registryConfig.getRegistryImpl().getAllProviderPaths().forEach(interfaceName -> {
                    createConsumerStub(interfaceName, registryConfig, providerProcessService, null, null);
                    registryConfig.getRegistryImpl().subscribeConsumerListener(interfaceName, consumerProcessService);
                });
                log.info("Found registry: [{}]", registryConfig.getRegistryUrl().getIdentity());
            });
        } catch (Exception e) {
            log.error("Failed to get provider or consumer", e);
        }
    }

    @Override
    public ConsumerStub<?> getConsumerStub(String registryIdentity, Url providerUrl) {
        Map<String, Object> attributes = new HashMap<>();
        if (StringUtils.isNotEmpty(providerUrl.getForm())) {
            attributes.put(FORM, providerUrl.getForm());
        }
        if (StringUtils.isNotEmpty(providerUrl.getVersion())) {
            attributes.put(VERSION, providerUrl.getVersion());
        }
        String beanName = ConsumerStub.buildConsumerStubBeanName(providerUrl.getPath(), attributes);
        if (ConsumerStubHolder.getInstance().get().containsKey(beanName)) {
            return ConsumerStubHolder.getInstance().get().get(beanName);
        }
        ConsumerStub<?> consumerStub = createConsumerStub(providerUrl.getPath(), findRegistryConfig(registryIdentity),
                null, providerUrl.getForm(), providerUrl.getVersion());
        ConsumerStubHolder.getInstance().add(beanName, consumerStub);
        return consumerStub;
    }

    private ConsumerStub<?> createConsumerStub(String interfaceName, RegistryConfig registryConfig,
                                               ProviderProcessable providerProcessService, String form, String version) {
        return ConsumerStub.create(interfaceName, infinityProperties.getApplication(), registryConfig,
                infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                providerProcessService, null, form, version, null, null);
    }

    @Override
    public List<RpcRegistryDTO> getRegistries() {
        return REGISTRIES;
    }

    @Override
    public Registry findRegistry(String urlIdentity) {
        return REGISTRY_CONFIG_MAP.get(urlIdentity).getRegistryImpl();
    }

    @Override
    public RegistryConfig findRegistryConfig(String urlIdentity) {
        return REGISTRY_CONFIG_MAP.get(urlIdentity);
    }
}
