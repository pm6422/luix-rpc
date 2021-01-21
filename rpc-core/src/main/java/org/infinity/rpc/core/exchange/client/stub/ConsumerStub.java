package org.infinity.rpc.core.exchange.client.stub;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.client.proxy.ConsumerProxy;
import org.infinity.rpc.core.constant.BooleanEnum;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.cluster.listener.SubscribeProviderListener;
import org.infinity.rpc.core.url.Url;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;

/**
 * PRC consumer stub
 * A stub in distributed computing is a piece of code that converts parameters passed between client and server
 * during a remote procedure call (RPC).
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
    @NotNull
    private final Class<T> interfaceClass;
    /**
     * Registry
     */
    private       String   registry;
    /**
     * Protocol
     */
    private       String   protocol;
    /**
     *
     */
    private       String   cluster;
    /**
     *
     */
    private       String   faultTolerance;
    /**
     *
     */
    private       String   loadBalancer;
    /**
     * Group
     */
    private       String   group;
    /**
     * Version
     */
    private       String   version;
    /**
     * Indicator to check health
     */
    private       Boolean  checkHealth;
    /**
     *
     */
    private       String   checkHealthFactory;
    /**
     *
     */
    private       int      requestTimeout;
    /**
     *
     */
    private       String   directUrl;

    /**
     * The consumer proxy instance, refer the return type of {@link ConsumerProxy#getProxy(ConsumerStub)}
     * Disable serialize
     */
    private transient final T                            proxyInstance;
    /**
     *
     */
    private                 Url                          clientUrl;
    /**
     *
     */
    private                 ProviderCluster<T>           providerCluster;
    /**
     * Disable serialize
     */
    private transient       SubscribeProviderListener<T> subscribeProviderListener;


    public ConsumerStub(Class<T> interfaceClass, Map<String, Object> annotationAttributesMap) {
        Validate.notNull(interfaceClass, "Consumer interface class must NOT be null!");

        this.interfaceClass = interfaceClass;
        // Set attribute values of @Consumer annotation
        setBasicField(annotationAttributesMap);
        this.proxyInstance = ConsumerProxy.getProxy(this);

        ConsumerStubHolder.getInstance().addStub(interfaceClass.getName(), this);
    }

    private void setBasicField(Map<String, Object> consumerAttributesMap) {
        registry = (String) consumerAttributesMap.get(REGISTRY);
        protocol = (String) consumerAttributesMap.get(PROTOCOL);
        cluster = (String) consumerAttributesMap.get(CLUSTER);
        faultTolerance = (String) consumerAttributesMap.get(FAULT_TOLERANCE);
        loadBalancer = (String) consumerAttributesMap.get(LOAD_BALANCER);
        group = (String) consumerAttributesMap.get(GROUP);
        version = (String) consumerAttributesMap.get(VERSION);
        BooleanEnum checkHealthEnum = (BooleanEnum) consumerAttributesMap.get(CHECK_HEALTH);
        checkHealth = checkHealthEnum.getValue();
        checkHealthFactory = (String) consumerAttributesMap.get(CHECK_HEALTH_FACTORY);
        requestTimeout = (int) consumerAttributesMap.get(REQUEST_TIMEOUT);
        directUrl = (String) consumerAttributesMap.get(DIRECT_URL);
    }

    public void init(List<Url> registryUrls) {
        this.clientUrl = Url.clientUrl(protocol, interfaceClass.getName());
        // Initialize provider cluster before consumer initialization
        this.providerCluster = createProviderCluster();
        subscribeProviderListener = SubscribeProviderListener.of(interfaceClass, providerCluster, registryUrls, clientUrl);
    }

    private ProviderCluster<T> createProviderCluster() {
        // todo: support multiple protocols
        // 当配置多个protocol的时候，比如A,B,C，
        // 那么正常情况下只会使用A，如果A被开关降级，那么就会使用B，B也被降级，那么会使用C
        // One cluster is created for one protocol, only one server node under a cluster can receive the request
        return ProviderCluster.createCluster(interfaceClass, cluster, protocol, faultTolerance, loadBalancer);
    }

//    @Override
//    public void destroy() {
//        // Leave blank intentionally for now
//    }
}
