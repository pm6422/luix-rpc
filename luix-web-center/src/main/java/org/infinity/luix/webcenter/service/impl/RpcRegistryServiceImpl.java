package org.infinity.luix.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.luix.core.client.stub.ConsumerStub;
import org.infinity.luix.core.client.stub.ConsumerStubFactory;
import org.infinity.luix.core.client.stub.ConsumerStubHolder;
import org.infinity.luix.core.config.impl.RegistryConfig;
import org.infinity.luix.core.listener.GlobalConsumerDiscoveryListener;
import org.infinity.luix.core.listener.GlobalProviderDiscoveryListener;
import org.infinity.luix.core.registry.Registry;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.spring.boot.config.LuixProperties;
import org.infinity.luix.webcenter.dto.RpcRegistryDTO;
import org.infinity.luix.webcenter.service.RpcRegistryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.infinity.luix.core.constant.ConsumerConstants.FAULT_TOLERANCE;
import static org.infinity.luix.core.constant.ProtocolConstants.SERIALIZER;
import static org.infinity.luix.core.constant.ServiceConstants.*;
import static org.infinity.luix.utilities.serializer.Serializer.SERIALIZER_NAME_HESSIAN2;

@Service
@Slf4j
public class RpcRegistryServiceImpl implements RpcRegistryService, ApplicationRunner {
    private static final Map<String, RegistryConfig>     REGISTRY_CONFIG_MAP = new ConcurrentHashMap<>();
    private static final List<RpcRegistryDTO>            REGISTRIES          = new ArrayList<>();
    @Resource
    private              LuixProperties                  luixProperties;
    @Resource
    private              GlobalConsumerDiscoveryListener globalConsumerDiscoveryListener;
    @Resource
    private              GlobalProviderDiscoveryListener globalProviderDiscoveryListener;

    /**
     * {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet()} execute too earlier
     *
     * @param args arguments
     * @throws Exception if any exception throws
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (CollectionUtils.isEmpty(luixProperties.getRegistryList())) {
            log.warn("No registries found!");
            return;
        }
        try {
            luixProperties.getRegistryList().forEach(registryConfig -> {
                REGISTRY_CONFIG_MAP.put(registryConfig.getRegistryUrl().getIdentity(), registryConfig);
                REGISTRIES.add(new RpcRegistryDTO(registryConfig.getRegistryImpl().getName(), registryConfig.getRegistryUrl().getIdentity()));

                registryConfig.getRegistryImpl().subscribe(globalProviderDiscoveryListener);

                List<Url> allProviderUrls = registryConfig.getRegistryImpl().discoverProviders();
                allProviderUrls.forEach(url -> {
                    try {
                        Map<String, Object> attributes = new HashMap<>(2);
                        attributes.put(FORM, url.getForm());
                        attributes.put(VERSION, url.getVersion());
                        String stubBeanName = ConsumerStub.buildConsumerStubBeanName(url.getPath(), attributes);
                        if (!ConsumerStubHolder.getInstance().getMap().containsKey(stubBeanName)) {
                            return;
                        }
                        // Generate consumer stub for each provider
                        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(luixProperties.getApplication(), registryConfig,
                                luixProperties.getAvailableProtocol(), url.getPath(), url.getForm());

                        ConsumerStubHolder.getInstance().add(stubBeanName, consumerStub);

                        // Register and active consumer services
                        consumerStub.registerAndActivate(luixProperties.getApplication(),
                                luixProperties.getAvailableProtocol(), registryConfig);
                    } catch (Exception e) {
                        log.error("Failed to create consumer stub for interface {}", url.getPath(), e);
                    }
                });

                // Register consumer changes processor
                registryConfig.getRegistryImpl().subscribe(globalConsumerDiscoveryListener);
            });
        } catch (Exception e) {
            log.error("Failed to create consumer stub!", e);
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
        String faultTolerance = null;
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
            faultTolerance = providerUrl.getOption(FAULT_TOLERANCE);
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
                    if (FAULT_TOLERANCE.equals(entry.getKey())) {
                        faultTolerance = entry.getValue();
                        attributesMap.put(entry.getKey(), faultTolerance);
                    }
                }
            }
        }

        String beanName = ConsumerStub.buildConsumerStubBeanName(resolvedInterfaceName, attributesMap);
        if (ConsumerStubHolder.getInstance().getMap().containsKey(beanName)) {
            // Return existing consumer stub
            return ConsumerStubHolder.getInstance().getMap().get(beanName);
        }

        // Default hessian 2 serializer
        String serializer = defaultIfEmpty(attributes.get(SERIALIZER), SERIALIZER_NAME_HESSIAN2);
        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(luixProperties.getApplication(),
                findRegistryConfig(registryIdentity), luixProperties.getAvailableProtocol(), providerAddress,
                resolvedInterfaceName, serializer, form, version, requestTimeout, retryCount, faultTolerance);

        ConsumerStubHolder.getInstance().add(beanName, consumerStub);

        if (StringUtils.isEmpty(providerUrlStr)) {
            // Non-direct address invocation needs to take time to discover addresses
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("Sleep interrupted!", e);
            }
        }
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
