package org.infinity.rpc.core.exchange.client.stub;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.client.proxy.ConsumerProxy;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.cluster.listener.SubscribeProviderListener;
import org.infinity.rpc.core.url.Url;

import javax.validation.constraints.NotEmpty;
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
@Getter
public class ConsumerStub<T> {
    /**
     * The interface class of the consumer
     */
    private final           Class<T>                     interfaceClass;
    /**
     * Registry
     */
    @NotEmpty
    private                 String                       registry;
    /**
     * Protocol
     */
    @NotEmpty
    private                 String                       protocol;
    /**
     * Group
     */
    @NotEmpty
    private                 String                       group;
    /**
     * Version
     */
    @NotEmpty
    private                 String                       version;
    /**
     * Indicator to check health
     */
    private                 boolean                      checkHealth;
    /**
     *
     */
    private                 String                       checkHealthFactory;
    /**
     *
     */
    private                 int                          timeout;
    /**
     * The consumer proxy instance, refer the return type of {@link ConsumerProxy#getProxy(ConsumerStub)}
     * Disable serialize
     */
    private transient final T                            proxyInstance;
    /**
     *
     */
    private                 ProviderCluster<T>           providerCluster;
    /**
     * Disable serialize
     */
    private transient       SubscribeProviderListener<T> subscribeProviderListener;
    /**
     *
     */
    private                 Url                          clientUrl;
    /**
     *
     */
    private                 String                       directUrl;

    public ConsumerStub(Class<T> interfaceClass) {
        Validate.notNull(interfaceClass, "Consumer interface class must NOT be null!");

        this.interfaceClass = interfaceClass;
        this.proxyInstance = ConsumerProxy.getProxy(this);
    }

    public void init(List<Url> registryUrls, Map<String, Object> consumerAttributesMap) {
        // Set attribute values of @Consumer annotation
        registry = (String) consumerAttributesMap.get(REGISTRY);
        protocol = (String) consumerAttributesMap.get(PROTOCOL);
        group = (String) consumerAttributesMap.get(GROUP);
        version = (String) consumerAttributesMap.get(VERSION);
        checkHealth = (boolean) consumerAttributesMap.get(CHECK_HEALTH);
        checkHealthFactory = (String) consumerAttributesMap.get(CHECK_HEALTH_FACTORY);
        timeout = (int) consumerAttributesMap.get(TIMEOUT);
        directUrl = (String) consumerAttributesMap.get(DIRECT_URL);

        clientUrl = Url.clientUrl((String) consumerAttributesMap.get(PROTOCOL), interfaceClass.getName());
        // Initialize provider cluster before consumer initialization
        providerCluster = createProviderCluster(consumerAttributesMap);
        subscribeProviderListener = SubscribeProviderListener.of(interfaceClass, providerCluster, registryUrls, clientUrl);
    }

    private ProviderCluster<T> createProviderCluster(Map<String, Object> consumerAttributesMap) {
        // todo: support multiple protocols
        // 当配置多个protocol的时候，比如A,B,C，
        // 那么正常情况下只会使用A，如果A被开关降级，那么就会使用B，B也被降级，那么会使用C
        // One cluster is created for one protocol, only one server node under a cluster can receive the request
        return ProviderCluster.createCluster(interfaceClass,
                (String) consumerAttributesMap.get(CLUSTER),
                (String) consumerAttributesMap.get(PROTOCOL),
                (String) consumerAttributesMap.get(FAULT_TOLERANCE),
                (String) consumerAttributesMap.get(LOAD_BALANCER));
    }

//    @Override
//    public void destroy() {
//        // Leave blank intentionally for now
//    }
}
