package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.infinity.rpc.core.constant.ProtocolConstants.SERIALIZER;
import static org.infinity.rpc.core.constant.ServiceConstants.*;
import static org.infinity.rpc.utilities.serializer.Serializer.SERIALIZER_NAME_HESSIAN2;

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
                    // First discover all consumers
                    registryConfig.getRegistryImpl().subscribeConsumerListener(interfaceName, consumerProcessService);
                    // Then discover all providers
                    ConsumerStub.create(interfaceName, infinityProperties.getApplication(), registryConfig,
                            infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                            providerProcessService);

                });
                log.info("Found registry: [{}]", registryConfig.getRegistryUrl().getIdentity());
            });
        } catch (Exception e) {
            log.error("Failed to get provider or consumer", e);
        }
    }

    @Override
    public ConsumerStub<?> getConsumerStub(String registryIdentity, String providerUrlStr, String interfaceName, Map<String, String> attributes) {
        Validate.isTrue(StringUtils.isNotEmpty(providerUrlStr) || StringUtils.isNotEmpty(interfaceName),
                "[providerUrl] and [interfaceName] can NOT be null at the same time!");

        String resolvedInterfaceName;
        String form = null;
        String version = null;
        Integer requestTimeout = null;
        Integer retryCount = null;
        String providerAddress = null;
        Map<String, Object> attributesMap = new HashMap<>(0);

        if (StringUtils.isNotEmpty(providerUrlStr)) {
            // Direct address invocation
            Url providerUrl = Url.valueOf(providerUrlStr);
            resolvedInterfaceName = providerUrl.getPath();
            form = providerUrl.getForm();
            version = providerUrl.getVersion();
            requestTimeout = providerUrl.containsOption(REQUEST_TIMEOUT) ? providerUrl.getIntOption(REQUEST_TIMEOUT) : null;
            retryCount = providerUrl.containsOption(RETRY_COUNT) ? providerUrl.getIntOption(RETRY_COUNT) : null;
            providerAddress = providerUrl.getAddress();
        } else {
            // Invocation after discovering addresses
            resolvedInterfaceName = interfaceName;
            if (MapUtils.isNotEmpty(attributes)) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    if (FORM.equals(entry.getKey())) {
                        form = entry.getValue();
                        attributesMap.put(entry.getKey(), form);
                    }
                    if (VERSION.equals(entry.getKey())) {
                        version = entry.getValue();
                        attributesMap.put(entry.getKey(), version);
                    }
                    if (REQUEST_TIMEOUT.equals(entry.getKey())) {
                        requestTimeout = entry.getValue() != null ? Integer.parseInt(entry.getValue()) : null;
                        attributesMap.put(entry.getKey(), requestTimeout);
                    }
                    if (RETRY_COUNT.equals(entry.getKey())) {
                        retryCount = entry.getValue() != null ? Integer.parseInt(entry.getValue()) : null;
                        attributesMap.put(entry.getKey(), retryCount);
                    }
                }
            }
        }

        String beanName = ConsumerStub.buildConsumerStubBeanName(resolvedInterfaceName, attributesMap);
        if (ConsumerStubHolder.getInstance().get().containsKey(beanName)) {
            return ConsumerStubHolder.getInstance().get().get(beanName);
        }

        // Default hessian 2 serializer
        String serializer = defaultIfEmpty(attributes.get(SERIALIZER), SERIALIZER_NAME_HESSIAN2);
        ConsumerStub<?> consumerStub = ConsumerStub.create(resolvedInterfaceName, infinityProperties.getApplication(),
                findRegistryConfig(registryIdentity), infinityProperties.getAvailableProtocol(), infinityProperties.getConsumer(),
                null, providerAddress, form, version, requestTimeout, retryCount, serializer);
        ConsumerStubHolder.getInstance().add(beanName, consumerStub);
        return consumerStub;
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
