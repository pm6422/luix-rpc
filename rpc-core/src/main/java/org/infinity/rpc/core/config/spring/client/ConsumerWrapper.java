package org.infinity.rpc.core.config.spring.client;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.ha.HighAvailability;
import org.infinity.rpc.core.exchange.loadbalancer.LoadBalancer;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PRC consumer configuration wrapper
 * And the class implements the {@link FactoryBean} interface means that
 * the class is used as a factory for an object to expose, not directly as a bean instance that will be exposed itself.
 */
@Slf4j
@Data
@Builder
public class ConsumerWrapper<T> implements DisposableBean {
    /**
     *
     */
    private InfinityProperties infinityProperties;
    /**
     * The interface class of the consumer
     */
    private Class<?>           interfaceClass;
    /**
     * The consumer instance simple name, also known as bean name
     */
    private String             instanceName;
    /**
     * The consumer proxy instance, refer the return type of {@link org.infinity.rpc.core.client.proxy.RpcConsumerProxy#getProxy(Class)}
     */
    private T                  proxyInstance;
    /**
     *
     */
    private List<Cluster<T>>   clusters;

    @Override
    public void destroy() {
        // Leave blank intentionally for now
    }

    public T getProxyInstance() {
        if (proxyInstance == null) {
            init();
        }
        return proxyInstance;
    }

    public synchronized void init() {
        // One cluster for one protocol
        // Only one server node under a cluster can receive the request
        clusters = new ArrayList<>(Arrays.asList(infinityProperties.getProtocol()).size());
        for (InfinityProperties.ProtocolConfig protocolConfig : Arrays.asList(infinityProperties.getProtocol())) {
            clusters.add(createCluster(protocolConfig));
        }
    }

    private Cluster<T> createCluster(InfinityProperties.ProtocolConfig protocolConfig) {
        Cluster<T> cluster = ServiceInstanceLoader.getServiceLoader(Cluster.class).load(protocolConfig.getCluster());
        LoadBalancer<T> loadBalancer = ServiceInstanceLoader.getServiceLoader(LoadBalancer.class).load(protocolConfig.getLoadBalancer());
        HighAvailability<T> ha = ServiceInstanceLoader.getServiceLoader(HighAvailability.class).load(protocolConfig.getHighAvailability());
//        ha.setProviderUrl();
        cluster.setLoadBalancer(loadBalancer);
        cluster.setHighAvailability(ha);
//        cluster.setProviderUrl();
        return cluster;
    }
}
