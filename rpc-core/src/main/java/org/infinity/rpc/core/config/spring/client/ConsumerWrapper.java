package org.infinity.rpc.core.config.spring.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.ha.HighAvailability;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.core.registry.RegistryConfig;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.network.NetworkIpUtils;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * PRC consumer configuration wrapper
 * And the class implements the {@link FactoryBean} interface means that
 * the class is used as a factory for an object to expose, not directly as a bean instance that will be exposed itself.
 */
@Slf4j
@Getter
public class ConsumerWrapper<T> implements DisposableBean {
    /**
     *
     */
    private InfinityProperties  infinityProperties;
    /**
     *
     */
    private RegistryConfig      registryConfig;
    /**
     * The interface class of the consumer
     */
    private Class<T>            interfaceClass;
    /**
     * The consumer instance simple name, also known as bean name
     */
    private String              instanceName;
    /**
     *
     */
    private RpcConsumerProxy<T> rpcConsumerProxy = new RpcConsumerProxy<>();
    /**
     * The consumer proxy instance, refer the return type of {@link org.infinity.rpc.core.client.proxy.RpcConsumerProxy#getProxy(Class, List, List)}
     */
    private T                   proxyInstance;
    /**
     *
     */
    private String              directUrl;
    /**
     *
     */
    private int                 timeout;
    /**
     *
     */
    private List<Cluster<T>>    clusters;

    public ConsumerWrapper(InfinityProperties infinityProperties, RegistryConfig registryConfig,
                           Class<T> interfaceClass, String instanceName, Map<String, Object> consumerAttributesMap) {
        this.infinityProperties = infinityProperties;
        this.registryConfig = registryConfig;
        this.interfaceClass = interfaceClass;
        this.instanceName = instanceName;
        this.directUrl = (String) consumerAttributesMap.get("directUrl");
        this.timeout = (int) consumerAttributesMap.get("timeout");

        // Initialize the consumer wrapper
        this.init();
    }

    public void init() {
        clusters = new ArrayList<>(Arrays.asList(infinityProperties.getProtocol()).size());
        for (InfinityProperties.ProtocolConfig protocolConfig : Arrays.asList(infinityProperties.getProtocol())) {
            // One cluster for one protocol, only one server node under a cluster can receive the request
            clusters.add(createCluster(protocolConfig));
        }

        proxyInstance = rpcConsumerProxy.getProxy(interfaceClass, clusters, registryConfig.getRegistries());
    }

    private Cluster<T> createCluster(InfinityProperties.ProtocolConfig protocolConfig) {
        Cluster<T> cluster = ServiceInstanceLoader.getServiceLoader(Cluster.class).load(protocolConfig.getCluster());
        LoadBalancer<T> loadBalancer = ServiceInstanceLoader.getServiceLoader(LoadBalancer.class).load(protocolConfig.getLoadBalancer());
        HighAvailability<T> ha = ServiceInstanceLoader.getServiceLoader(HighAvailability.class).load(protocolConfig.getHighAvailability());
        ha.setClientUrl(Url.clientUrl(protocolConfig.getName().name(), NetworkIpUtils.INTRANET_IP, interfaceClass.getName()));

        cluster.setLoadBalancer(loadBalancer);
        cluster.setHighAvailability(ha);
        return cluster;
    }

    @Override
    public void destroy() {
        // Leave blank intentionally for now
    }
}
