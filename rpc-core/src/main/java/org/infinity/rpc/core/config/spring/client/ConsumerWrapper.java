package org.infinity.rpc.core.config.spring.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.proxy.ConsumerProxy;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.exchange.cluster.listener.ConsumerListener;
import org.infinity.rpc.core.url.Url;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import java.util.List;
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
     * The name of consumer wrapper instance
     */
    private final String              consumerWrapperBeanName;
    /**
     * The interface class of the consumer
     */
    private final Class<T>            interfaceClass;
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
    /**
     *
     */
    private       String              directUrl;
    /**
     *
     */
    private       int                 timeout;


    public ConsumerWrapper(String consumerWrapperBeanName, Class<T> interfaceClass) {
        this.consumerWrapperBeanName = consumerWrapperBeanName;
        this.interfaceClass = interfaceClass;
    }

    public void init(InfinityProperties infinityProperties, List<Url> registryUrls, Map<String, Object> consumerAttributesMap) {
        proxyInstance = ConsumerProxy.getProxy(interfaceClass, infinityProperties);
        clientUrl = Url.clientUrl(infinityProperties.getProtocol().getName().name(), interfaceClass.getName());
        consumerListener = ConsumerListener.of(interfaceClass, registryUrls, clientUrl);
        // Set attribute values of @Consumer annotation
        directUrl = (String) consumerAttributesMap.get("directUrl");
        timeout = (int) consumerAttributesMap.get("timeout");
    }

    @Override
    public void destroy() {
        // Leave blank intentionally for now
    }
}
