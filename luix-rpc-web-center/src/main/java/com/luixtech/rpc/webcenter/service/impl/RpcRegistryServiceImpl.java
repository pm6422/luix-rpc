package com.luixtech.rpc.webcenter.service.impl;

import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.core.client.stub.ConsumerStubFactory;
import com.luixtech.rpc.core.client.stub.ConsumerStubHolder;
import com.luixtech.rpc.core.config.impl.RegistryConfig;
import com.luixtech.rpc.core.listener.GlobalConsumerDiscoveryListener;
import com.luixtech.rpc.core.listener.GlobalProviderDiscoveryListener;
import com.luixtech.rpc.core.registry.Registry;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.spring.boot.starter.config.LuixRpcProperties;
import com.luixtech.rpc.webcenter.dto.RpcRegistryDTO;
import com.luixtech.rpc.webcenter.service.RpcRegistryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.luixtech.rpc.core.constant.ConsumerConstants.FAULT_TOLERANCE;
import static com.luixtech.rpc.core.constant.ProtocolConstants.SERIALIZER;
import static com.luixtech.rpc.core.constant.ServiceConstants.*;
import static com.luixtech.rpc.serializer.Serializer.SERIALIZER_NAME_HESSIAN2;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

@Service
@AllArgsConstructor
@Slf4j
public class RpcRegistryServiceImpl implements RpcRegistryService, ApplicationRunner {
    private static final Map<String, RegistryConfig>     REGISTRY_CONFIG_MAP = new ConcurrentHashMap<>();
    private static final List<RpcRegistryDTO>            REGISTRIES          = new ArrayList<>();
    private final        LuixRpcProperties               luixRpcProperties;
    private final        GlobalConsumerDiscoveryListener globalConsumerDiscoveryListener;
    private final        GlobalProviderDiscoveryListener globalProviderDiscoveryListener;

    /**
     * {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet()} execute too earlier
     *
     * @param args arguments
     * @throws Exception if any exception throws
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (CollectionUtils.isEmpty(luixRpcProperties.getRegistryList())) {
            log.warn("No registries found!");
            return;
        }
        try {
            luixRpcProperties.getRegistryList().forEach(registryConfig -> {
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
                        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(luixRpcProperties.getApplication(), registryConfig,
                                luixRpcProperties.getAvailableProtocol(), url.getPath(), url.getForm());

                        ConsumerStubHolder.getInstance().add(stubBeanName, consumerStub);

                        // Register and active consumer services
                        consumerStub.registerAndActivate(luixRpcProperties.getApplication(),
                                luixRpcProperties.getAvailableProtocol(), registryConfig);
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
        ConsumerStub<?> consumerStub = ConsumerStubFactory.create(luixRpcProperties.getApplication(),
                findRegistryConfig(registryIdentity), luixRpcProperties.getAvailableProtocol(), providerAddress,
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
