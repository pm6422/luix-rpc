package org.infinity.rpc.core.config.spring.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.ConsumerProxy;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.exchange.cluster.listener.ConsumerListener;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.infinity.rpc.core.url.Url;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import java.util.Map;

/**
 * PRC consumer configuration wrapper
 * And the class implements the {@link FactoryBean} interface means that
 * the class is used as a factory for an object to expose, not directly as a bean instance that will be exposed itself.
 *
 * @param <T>: The interface class of the consumer
 */
@Slf4j
@Getter
public class ConsumerWrapper<T> implements DisposableBean {
    /**
     *
     */
    private final InfinityProperties  infinityProperties;
    /**
     *
     */
    private final RegistryInfo        registryInfo;
    /**
     * The interface class of the consumer
     */
    private final Class<T>            interfaceClass;
    /**
     * The consumer instance simple name, also known as bean name
     */
    private final String              instanceName;
    /**
     *
     */
    private final String              directUrl;
    /**
     *
     */
    private final int                 timeout;
    /**
     * The consumer proxy instance, refer the return type of {@link ConsumerProxy#getProxy(Class, InfinityProperties)}
     */
    private       T                   proxyInstance;
    /**
     *
     */
    private       ConsumerListener<T> consumerListener;
    /**
     *
     */
    private       Url                 clientUrl;

    public ConsumerWrapper(InfinityProperties infinityProperties, RegistryInfo registryInfo,
                           Class<T> interfaceClass, String instanceName, Map<String, Object> consumerAttributesMap) {
        this.infinityProperties = infinityProperties;
        this.registryInfo = registryInfo;
        this.interfaceClass = interfaceClass;
        this.instanceName = instanceName;
        this.directUrl = (String) consumerAttributesMap.get("directUrl");
        this.timeout = (int) consumerAttributesMap.get("timeout");

        // @TODO invoke outside
        this.init();
    }

    public void init() {
        clientUrl = Url.clientUrl(infinityProperties.getProtocol().getName().name(), interfaceClass.getName());
        consumerListener = ConsumerListener.of(interfaceClass, registryInfo.getRegistryUrls(), clientUrl);
        proxyInstance = ConsumerProxy.getProxy(interfaceClass, infinityProperties);
    }

    @Override
    public void destroy() {
        // Leave blank intentionally for now
    }
}
