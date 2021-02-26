package org.infinity.rpc.core.client.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.rpc.core.client.cluster.ProviderCluster;
import org.infinity.rpc.core.client.listener.ProviderDiscoveryListener;
import org.infinity.rpc.core.client.listener.ProviderNotifyListener;
import org.infinity.rpc.core.client.proxy.ProxyFactory;
import org.infinity.rpc.core.client.proxy.impl.JdkProxyFactory;
import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.network.AddressUtils;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.infinity.rpc.core.constant.ProtocolConstants.*;
import static org.infinity.rpc.core.constant.RegistryConstants.REGISTRY_VAL_DIRECT;
import static org.infinity.rpc.core.constant.ServiceConstants.*;

/**
 * PRC consumer stub
 * A stub in distributed computing is a piece of code that converts parameters passed between client and server
 * during a remote procedure call(RPC).
 * A consumer stub take charge of creating a proxy instance of the consumer interface class, any method invocation
 * will be delegated to the proxy instance.
 *
 * @param <T>: The interface class of the consumer
 */
@Slf4j
@Setter
@Getter
public class ConsumerStub<T> {
    /**
     * The interface class of the consumer
     */
    @NotNull(message = "The [interfaceClass] property of @Consumer must NOT be null!")
    private Class<T> interfaceClass;
    /**
     * The provider interface fully-qualified name
     */
    @NotEmpty(message = "The [interfaceName] property of @Consumer must NOT be empty!")
    private String   interfaceName;
    /**
     * Protocol
     */
    private String   protocol;
//    /**
//     * 多注册中心，所以不能使用单个
//     * Registry
//     */
//    private String   registry;
    /**
     * Provider caller cluster
     */
    private String   cluster;
    /**
     *
     */
    private String   faultTolerance;
    /**
     *
     */
    private String   loadBalancer;
    /**
     * Group
     */
    private String   group;
    /**
     * Version
     */
    private String   version;
    /**
     * Consumer proxy factory used to create {@link #proxyInstance} which is the implementation of consumer interface class
     */
    private String   proxyFactory;
    /**
     *
     */
    private String   checkHealthFactory;
    /**
     * The field name must be identical to the field of {@link org.infinity.rpc.core.server.annotation.Provider}
     */
    @Min(value = 0, message = "The [timeout] property of @Consumer must NOT be a negative number!")
    private Integer  requestTimeout;
    /**
     * The max retry times of RPC request
     * The field name must be identical to the field of {@link org.infinity.rpc.core.server.annotation.Provider}
     */
    @Min(value = 0, message = "The [maxRetries] property of @Consumer must NOT be a negative number!")
    @Max(value = 10, message = "The [maxRetries] property of @Consumer must NOT be less than 10!")
    private Integer  maxRetries;
    /**
     * Addresses of RPC provider used to connect RPC provider directly without third party registry.
     * Multiple addresses are separated by comma.
     */
    private String   directAddresses;
    /**
     *
     */
    private boolean  generic;
    /**
     * The consumer url used to export to registry only for consumers discovery management,
     * but it have nothing to do with the service calling.
     * todo: 暂时无用
     */
    private Url      url;

    /**
     * The consumer proxy instance, refer the return type of {@link JdkProxyFactory#getProxy(ConsumerStub)}
     * Disable serialize
     */
    private transient T                  proxyInstance;
    /**
     *
     */
    private           Url                clientUrl;
    /**
     *
     */
    private           ProviderCluster<T> providerCluster;

    /**
     * The method is invoked by Java EE container automatically after registered bean definition
     */
    @PostConstruct
    public void init() {
        this.proxyInstance = ProxyFactory.getInstance(proxyFactory).getProxy(this);
        // Automatically add {@link ConsumerStub} instance to {@link ConsumerStubHolder}
        ConsumerStubHolder.getInstance().addStub(this);
    }

    /**
     * Subscribe the RPC providers from registry
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registry          registry
     */
    public void subscribeProviders(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig, RegistryConfig registry) {
        subscribeProviders(applicationConfig, protocolConfig, Collections.singleton(registry));
    }

    /**
     * Subscribe the RPC providers from multiple registries
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param registries        registries
     */
    public void subscribeProviders(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig,
                                   Collection<RegistryConfig> registries) {
        List<Url> registryUrls = registries
                .stream()
                .map(registryConfig -> registryConfig.getRegistryUrl())
                .collect(Collectors.toList());

        // Create consumer url
        url = this.createConsumerUrl(applicationConfig, protocolConfig);

        // We do NOT know the host and port of provider right now, so we use client URL
        clientUrl = Url.clientUrl(protocol, protocolConfig.getHost(), interfaceName, group, version);
        clientUrl.addOption(THROW_EXCEPTION, String.valueOf(protocolConfig.isThrowException()));

        // Initialize provider cluster before consumer initialization
        providerCluster = createProviderCluster();

        if (StringUtils.isEmpty(directAddresses)) {
            // Non-direct registry
            // Pass provider cluster to listener, listener will update provider cluster after provider urls changed
            ProviderDiscoveryListener<T> listener = ProviderDiscoveryListener.of(providerCluster, interfaceClass, clientUrl);
            listener.subscribe(registryUrls);
            return;
        }

        // Direct registry
        notifyDirectProviderUrls(protocolConfig, registryUrls);
    }

    /**
     * Merge high priority properties to consumer stub and generate consumer url
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @return consumer url
     */
    private Url createConsumerUrl(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig) {
        url = Url.consumerUrl(protocol, protocolConfig.getHost(), protocolConfig.getPort(), interfaceName, group, version);

        url.addOption(Url.PARAM_APP, applicationConfig.getName());
        url.addOption(CODEC, protocolConfig.getCodec());
        url.addOption(LOCAL_ADDRESS_FACTORY, protocolConfig.getLocalAddressFactory());

        url.addOption(CHECK_HEALTH_FACTORY, checkHealthFactory);
        url.addOption(REQUEST_TIMEOUT, requestTimeout != null ? requestTimeout.toString() : null);
        url.addOption(MAX_RETRIES, maxRetries != null ? maxRetries.toString() : null);
        return url;
    }

    private void notifyDirectProviderUrls(ProtocolConfig protocolConfig, List<Url> globalRegistryUrls) {
        // Pass provider cluster to listener, listener will update provider cluster after provider urls changed
        ProviderNotifyListener<T> listener = ProviderNotifyListener.of(providerCluster, interfaceClass, protocol);

        for (Url globalRegistryUrl : globalRegistryUrls) {
            List<Url> directProviderUrls = createDirectProviderUrls(protocolConfig);
            Url directRegistryUrl = globalRegistryUrl.copy();
            // Change protocol to direct
            directRegistryUrl.setProtocol(REGISTRY_VAL_DIRECT);
            // 如果有directUrls，直接使用这些directUrls进行初始化，不用到注册中心discover
            // Directly notify the provider urls
            listener.onNotify(directRegistryUrl, directProviderUrls);
            log.info("Notified registries [{}] with direct provider urls {}", directRegistryUrl, directProviderUrls);
        }
    }

    private List<Url> createDirectProviderUrls(ProtocolConfig protocolConfig) {
        // Get the provider host and port
        List<Pair<String, Integer>> directUrlHostPortList = AddressUtils.parseAddress(directAddresses);
        List<Url> directProviderUrls = new ArrayList<>(directUrlHostPortList.size());
        for (Pair<String, Integer> providerHostPortPair : directUrlHostPortList) {
            // Note: There are no extra options added to the direct provider url
            Url providerUrl = Url.providerUrl(PROTOCOL_VAL_INFINITY, providerHostPortPair.getLeft(),
                    providerHostPortPair.getRight(), interfaceName, group, version);
            // Please refer to ProviderStub for direct provider url options
            providerUrl.addOption(CODEC, protocolConfig.getCodec());
            providerUrl.addOption(LOCAL_ADDRESS_FACTORY, protocolConfig.getLocalAddressFactory());
            providerUrl.addOption(MIN_CLIENT_CONN, String.valueOf(protocolConfig.getMinClientConn()));
            directProviderUrls.add(providerUrl);
        }
        return directProviderUrls;
    }

    private ProviderCluster<T> createProviderCluster() {
        // One cluster is created for one protocol, only one server node under a cluster can receive the request
        return ProviderCluster.createCluster(interfaceClass, cluster, protocol, faultTolerance, loadBalancer, clientUrl);
    }
}
