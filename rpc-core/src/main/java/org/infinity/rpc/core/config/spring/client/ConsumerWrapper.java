package org.infinity.rpc.core.config.spring.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.exchange.cluster.listener.ConsumerListener;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.infinity.rpc.core.url.Url;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

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
    private RegistryInfo        registryInfo;
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
     * The consumer proxy instance, refer the return type of {@link org.infinity.rpc.core.client.proxy.RpcConsumerProxy#getProxy(Class, List, List, InfinityProperties)}
     */
    private T                   proxyInstance;
    /**
     *
     */
    private ConsumerListener    consumerListener;
    /**
     *
     */
    private Url                 clientUrl;
    /**
     *
     */
    private String              directUrl;
    /**
     *
     */
    private int                 timeout;

    public ConsumerWrapper(InfinityProperties infinityProperties, RegistryInfo registryInfo,
                           Class<T> interfaceClass, String instanceName, Map<String, Object> consumerAttributesMap) {
        this.infinityProperties = infinityProperties;
        this.registryInfo = registryInfo;
        this.interfaceClass = interfaceClass;
        this.instanceName = instanceName;
        this.directUrl = (String) consumerAttributesMap.get("directUrl");
        this.timeout = (int) consumerAttributesMap.get("timeout");

        // Initialize the consumer wrapper
        this.init();
    }

    public void init() {
        clientUrl = Url.clientUrl(infinityProperties.getProtocol().getName().name(), interfaceClass.getName());
        consumerListener = ConsumerListener.of(interfaceClass, registryInfo.getRegistryUrls(), clientUrl);
        proxyInstance = rpcConsumerProxy.getProxy(interfaceClass, registryInfo.getRegistries(), infinityProperties);
    }

    @Override
    public void destroy() {
        // Leave blank intentionally for now
    }
}
