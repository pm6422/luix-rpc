package org.infinity.rpc.core.exchange.client.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.proxy.ConsumerProxy;
import org.infinity.rpc.core.config.ConsumerConfig;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.cluster.listener.SubscribeProviderListener;
import org.infinity.rpc.core.url.Url;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

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
     *
     */
    private boolean  generic;
    /**
     * Protocol
     */
    private String   protocol;
    /**
     * Registry
     */
    private String   registry;
    /**
     *
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
     *
     */
    private String   directUrl;

    /**
     * The consumer proxy instance, refer the return type of {@link ConsumerProxy#getProxy(ConsumerStub)}
     * Disable serialize
     */
    private transient T                            proxyInstance;
    /**
     *
     */
    private           Url                          clientUrl;
    /**
     *
     */
    private           ProviderCluster<T>           providerCluster;
    /**
     * Disable serialize
     */
    private transient SubscribeProviderListener<T> subscribeProviderListener;

    /**
     * The method is invoked by Java EE container automatically after registered bean definition
     * Automatically add {@link ConsumerStub} instance to {@link ConsumerStubHolder}
     */
    @PostConstruct
    public void init() {
        this.proxyInstance = ConsumerProxy.getProxy(this);
        ConsumerStubHolder.getInstance().addStub(interfaceClass.getName(), this);
    }

    /**
     * Merge attributes with high priority properties to consumer stub
     *
     * @param protocolConfig protocol configuration
     * @param registryConfig registry configuration
     * @param consumerConfig consumer configuration
     */
    public void mergeAttributes(ProtocolConfig protocolConfig,
                                RegistryConfig registryConfig,
                                ConsumerConfig consumerConfig) {
        if (StringUtils.isEmpty(protocol)) {
            protocol = protocolConfig.getName();
        }
        if (StringUtils.isEmpty(registry)) {
            registry = registryConfig.getName();
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
        if (StringUtils.isEmpty(group)) {
            group = consumerConfig.getGroup();
        }
        if (StringUtils.isEmpty(version)) {
            version = consumerConfig.getVersion();
        }
        if (checkHealth == null) {
            consumerConfig.isCheckHealth();
        }
        if (StringUtils.isEmpty(checkHealthFactory)) {
            checkHealthFactory = consumerConfig.getCheckHealthFactory();
        }
        if (Integer.MAX_VALUE == requestTimeout) {
            requestTimeout = consumerConfig.getRequestTimeout();
        }
    }

    /**
     * Subscribe the RPC providers from registries
     *
     * @param registryUrls registry urls
     */
    public void subscribeFromRegistries(Url... registryUrls) {
        this.clientUrl = Url.clientUrl(protocol, interfaceClass.getName());
        // Initialize provider cluster before consumer initialization
        this.providerCluster = createProviderCluster();
        subscribeProviderListener = SubscribeProviderListener.of(interfaceClass, providerCluster, clientUrl, registryUrls);
    }

    private ProviderCluster<T> createProviderCluster() {
        // One cluster is created for one protocol, only one server node under a cluster can receive the request
        return ProviderCluster.createCluster(interfaceClass, cluster, protocol, faultTolerance, loadBalancer);
    }
}
