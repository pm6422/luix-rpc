package org.infinity.rpc.core.exchange.client.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.infinity.rpc.core.client.proxy.ConsumerProxy;
import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.core.config.ConsumerConfig;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.cluster.listener.SubscribeProviderListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.url.UrlUtils;
import org.infinity.rpc.utilities.network.AddressUtils;
import org.infinity.rpc.utilities.network.NetworkUtils;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.infinity.rpc.core.constant.ConsumerConstants.DIRECT_URLS;
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
     * The provider interface fully-qualified name
     */
    @NotEmpty(message = "The [interfaceName] property of @Consumer must NOT be empty!")
    private String   interfaceName;
    /**
     * The interface class of the consumer
     */
    @NotNull(message = "The [interfaceClass] property of @Consumer must NOT be null!")
    private Class<T> interfaceClass;
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
     * Indicator to check health
     * Note: It must be specified with Boolean wrapper class
     */
    private Boolean  checkHealth;
    /**
     *
     */
    private String   checkHealthFactory;
    /**
     * The field name must be identical to the field of {@link org.infinity.rpc.core.server.annotation.Provider}
     */
    @Min(value = 0, message = "The [timeout] property of @Consumer must NOT be a negative number!")
    private int      requestTimeout;
    /**
     * The max retry times of RPC request
     * The field name must be identical to the field of {@link org.infinity.rpc.core.server.annotation.Provider}
     */
    @Min(value = 0, message = "The [maxRetries] property of @Consumer must NOT be a negative number!")
    private int      maxRetries;
    /**
     * Direct provider urls
     */
    private String   directUrls;
    /**
     *
     */
    private boolean  generic;
    /**
     * The consumer url used to export to registry only for consumers discovery management,
     * but it have nothing to do with the service calling.
     */
    private Url      url;

    /**
     * The consumer proxy instance, refer the return type of {@link ConsumerProxy#getProxy(ConsumerStub)}
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
     * Automatically add {@link ConsumerStub} instance to {@link ConsumerStubHolder}
     */
    @PostConstruct
    public void init() {
        this.proxyInstance = ConsumerProxy.getProxy(this);
        ConsumerStubHolder.getInstance().addStub(this);
    }

    /**
     * Subscribe the RPC providers from registries
     *
     * @param applicationConfig  application configuration
     * @param protocolConfig     protocol configuration
     * @param globalRegistryUrls global registry urls
     * @param consumerConfig     consumer configuration
     */
    public void subscribeFromRegistries(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig,
                                        List<Url> globalRegistryUrls, ConsumerConfig consumerConfig) {
        // Create consumer url
        this.url = this.createConsumerUrl(applicationConfig, protocolConfig, consumerConfig);

        this.clientUrl = Url.clientUrl(protocol, interfaceName, group, version);
        // Initialize provider cluster before consumer initialization
        this.providerCluster = createProviderCluster();

        if (StringUtils.isEmpty(directUrls)) {
            // Non-direct registry
            SubscribeProviderListener.of(interfaceClass, providerCluster, clientUrl, globalRegistryUrls);
            return;
        }

        // Direct registry
        List<Url> directRegistryUrls = createDirectRegistryUrls(globalRegistryUrls);
        SubscribeProviderListener<T> listener = SubscribeProviderListener.of(interfaceClass, providerCluster, clientUrl, directRegistryUrls);
        directRegistryUrls.forEach(registryUrl -> {
            // 如果有directUrls，直接使用这些directUrls进行初始化，不用到注册中心discover
            List<Url> directProviderUrls = UrlUtils.parseDirectUrls(registryUrl.getOption(DIRECT_URLS));
            // Directly notify the provider urls
            listener.onNotify(registryUrl, directProviderUrls);
            log.info("Notified registries [{}] with direct provider urls {}", registryUrl, directProviderUrls);
        });
    }

    /**
     * Merge high priority properties to consumer stub and generate consumer url
     *
     * @param applicationConfig application configuration
     * @param protocolConfig    protocol configuration
     * @param consumerConfig    consumer configuration
     * @return provider url
     */
    private Url createConsumerUrl(ApplicationConfig applicationConfig, ProtocolConfig protocolConfig, ConsumerConfig consumerConfig) {
        if (StringUtils.isEmpty(protocol)) {
            protocol = protocolConfig.getName();
        }
//        if (StringUtils.isEmpty(registry)) {
//            registry = registryConfig.getName();
//        }
        if (StringUtils.isEmpty(group)) {
            group = consumerConfig.getGroup();
        }
        if (StringUtils.isEmpty(version)) {
            version = consumerConfig.getVersion();
        }
        if (StringUtils.isEmpty(cluster)) {
            cluster = consumerConfig.getCluster();
        }
        if (StringUtils.isEmpty(faultTolerance)) {
            faultTolerance = consumerConfig.getFaultTolerance();
        }
        if (StringUtils.isEmpty(loadBalancer)) {
            loadBalancer = consumerConfig.getLoadBalancer();
        }

        // todo: check internet or intranet ip
        url = Url.consumerUrl(protocol, NetworkUtils.INTRANET_IP, protocolConfig.getPort(), interfaceName, group, version);
        url.addOption(Url.PARAM_APP, applicationConfig.getName());

        if (checkHealth == null) {
            checkHealth = consumerConfig.isCheckHealth();
        }
        url.addOption(CHECK_HEALTH, String.valueOf(checkHealth));

        if (StringUtils.isEmpty(checkHealthFactory)) {
            checkHealthFactory = consumerConfig.getCheckHealthFactory();
        }
        url.addOption(CHECK_HEALTH_FACTORY, checkHealthFactory);

        if (Integer.MAX_VALUE == requestTimeout) {
            requestTimeout = consumerConfig.getRequestTimeout();
        }
        url.addOption(REQUEST_TIMEOUT, String.valueOf(requestTimeout));

        if (Integer.MAX_VALUE == maxRetries) {
            maxRetries = consumerConfig.getMaxRetries();
        }
        url.addOption(MAX_RETRIES, String.valueOf(maxRetries));

        return url;
    }

    private List<Url> createDirectRegistryUrls(List<Url> globalRegistryUrls) {
        List<Url> urls = new ArrayList<>(globalRegistryUrls.size());
        for (Url globalRegistryUrl : globalRegistryUrls) {
            Url url = globalRegistryUrl.copy();
            // Change protocol to direct
            url.setProtocol(REGISTRY_VALUE_DIRECT);

            String directProviderUrls = createDirectProviderUrls();
            url.addOption(DIRECT_URLS, directProviderUrls);
            urls.add(url);
        }
        return urls;
    }

    private String createDirectProviderUrls() {
        StringBuilder directProviderUrls = new StringBuilder(128);
        List<Pair<String, Integer>> directUrlHostPortList = AddressUtils.parseAddress(directUrls);
        Iterator<Pair<String, Integer>> iterator = directUrlHostPortList.iterator();
        while (iterator.hasNext()) {
            Pair<String, Integer> providerHostPortPair = iterator.next();
            Url providerUrl = Url.providerUrl(url.getProtocol(), providerHostPortPair.getLeft(),
                    providerHostPortPair.getRight(), url.getPath(), url.getGroup(), url.getVersion());
            // We can NOT get the provider url via direct url mode
            providerUrl.addOption(Url.PARAM_APP, Url.PARAM_APP_UNKNOWN);
            providerUrl.addOption(Url.PARAM_TYPE, Url.PARAM_TYPE_PROVIDER);
            directProviderUrls.append(providerUrl.toFullStr());
            if (iterator.hasNext()) {
                directProviderUrls.append(RpcConstants.COMMA_SEPARATOR);
            }
        }
        return directProviderUrls.toString();
    }

    private ProviderCluster<T> createProviderCluster() {
        // One cluster is created for one protocol, only one server node under a cluster can receive the request
        return ProviderCluster.createCluster(interfaceClass, cluster, protocol, faultTolerance, loadBalancer);
    }
}
