package org.infinity.rpc.spring.boot.client;

import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.core.constant.BooleanEnum;
import org.infinity.rpc.core.exchange.client.stub.ConsumerStub;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.infinity.rpc.spring.boot.bean.processor.AbstractAnnotationBeanPostProcessor;
import org.infinity.rpc.spring.boot.client.stub.ConsumerBeanNameBuilder;
import org.infinity.rpc.spring.boot.client.stub.ConsumerStubBeanNameBuilder;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.spring.boot.utils.AnnotationUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.spring.boot.utils.AnnotationUtils.getResolvedAttributes;

public class ConsumerAnnotationBeanPostProcessor extends AbstractAnnotationBeanPostProcessor implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public ConsumerAnnotationBeanPostProcessor() {
        // Set the target annotation to be injected to spring context
        super(Consumer.class);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
                                                 Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        String interfaceName = AnnotationUtils.resolveInterfaceName(attributes, injectedType);
        return buildConsumerBeanName(interfaceName, attributes) +
                "#source=" + (injectedElement.getMember()) +
                "#attributes=" + getResolvedAttributes(attributes, getEnvironment());
    }

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> consumerInterfaceClass,
                                       InjectionMetadata.InjectedElement injectedElement) {
        String resolvedConsumerInterfaceName = AnnotationUtils.resolveInterfaceName(attributes, consumerInterfaceClass);
        String consumerStubBeanName = buildConsumerStubBeanName(resolvedConsumerInterfaceName, attributes);

        if (existsBean(consumerStubBeanName)) {
            // Return the instance if it already be registered
            return applicationContext.getBean(consumerStubBeanName, ConsumerStub.class).getProxyInstance();
        }

        ConsumerStub<?> consumerStub = buildConsumerStub(consumerInterfaceClass, attributes);
        // Register the consumer stub instance with singleton scope
        beanFactory.registerSingleton(consumerStubBeanName, consumerStub);
        return consumerStub.getProxyInstance();
    }

    /**
     * Build the consumer bean name
     *
     * @param interfaceName the name of consumer interface
     * @param attributes    the attributes of {@link Consumer @Consumer}
     * @return The name of bean that annotated with {@link Consumer @Consumer} in local spring {@link ApplicationContext}
     */
    private String buildConsumerBeanName(String interfaceName, AnnotationAttributes attributes) {
        return ConsumerBeanNameBuilder
                .builder(interfaceName, env)
                .group(attributes.getString(GROUP))
                .version(attributes.getString(VERSION)).build();
    }

    /**
     * Build the consumer stub bean name
     *
     * @param interfaceName the name of consumer interface
     * @param attributes    the attributes of {@link Consumer @Consumer}
     * @return The name of bean that annotated with {@link Consumer @Consumer} in spring {@link ApplicationContext}
     */
    private String buildConsumerStubBeanName(String interfaceName, AnnotationAttributes attributes) {
        return ConsumerStubBeanNameBuilder
                .builder(interfaceName, env)
                .group(attributes.getString(GROUP))
                .version(attributes.getString(VERSION)).build();
    }

    private boolean existsBean(String beanName) {
        return applicationContext.containsBean(beanName);
    }

    private ConsumerStub<?> buildConsumerStub(Class<?> interfaceClass, AnnotationAttributes annotationAttributes) {
        InfinityProperties infinityProperties = applicationContext.getBean(InfinityProperties.class);
        RegistryInfo registryInfo = applicationContext.getBean(RegistryInfo.class);
        Map<String, Object> mergedAttributesMap = mergeAttributesMap(annotationAttributes, infinityProperties);
        ConsumerStub<?> consumerStub = new ConsumerStub<>(interfaceClass, mergedAttributesMap);
        consumerStub.createSubscribeProviderListener(registryInfo.getRegistryUrls());
        return consumerStub;
    }

    /**
     * Consumer configuration takes priority over global one
     *
     * @param annotationAttributes annotation attributes from @Consumer annotation
     * @param infinityProperties   configuration properties
     * @return resolved attributes
     */
    private Map<String, Object> mergeAttributesMap(AnnotationAttributes annotationAttributes,
                                                   InfinityProperties infinityProperties) {
        // Copy @Consumer annotation attributes to the map first
        Map<String, Object> consumerAttributesMap = new HashMap<>(annotationAttributes);

        String registry = defaultIfEmpty(annotationAttributes.getString(REGISTRY), infinityProperties.getRegistry().getName());
        consumerAttributesMap.put(REGISTRY, registry);

        String protocol = defaultIfEmpty(annotationAttributes.getString(PROTOCOL), infinityProperties.getProtocol().getName());
        consumerAttributesMap.put(PROTOCOL, protocol);

        String cluster = defaultIfEmpty(annotationAttributes.getString(CLUSTER), infinityProperties.getConsumer().getCluster());
        consumerAttributesMap.put(CLUSTER, cluster);

        String faultTolerance = defaultIfEmpty(annotationAttributes.getString(FAULT_TOLERANCE), infinityProperties.getConsumer().getFaultTolerance());
        consumerAttributesMap.put(FAULT_TOLERANCE, faultTolerance);

        String loadBalancer = defaultIfEmpty(annotationAttributes.getString(LOAD_BALANCER), infinityProperties.getConsumer().getLoadBalancer());
        consumerAttributesMap.put(LOAD_BALANCER, loadBalancer);

        String group = defaultIfEmpty(annotationAttributes.getString(GROUP), infinityProperties.getConsumer().getGroup());
        consumerAttributesMap.put(GROUP, group);

        String version = defaultIfEmpty(annotationAttributes.getString(VERSION), infinityProperties.getConsumer().getVersion());
        consumerAttributesMap.put(VERSION, version);

        BooleanEnum checkHealthEnum = annotationAttributes.getEnum(CHECK_HEALTH);
        boolean checkHealth = toBooleanDefaultIfNull(checkHealthEnum.getValue(), infinityProperties.getConsumer().isCheckHealth());
        consumerAttributesMap.put(CHECK_HEALTH, checkHealth);

        String checkHealthFactory = defaultIfEmpty(annotationAttributes.getString(CHECK_HEALTH_FACTORY),
                infinityProperties.getConsumer().getCheckHealthFactory());
        consumerAttributesMap.put(CHECK_HEALTH_FACTORY, checkHealthFactory);

        Number requestTimeoutNum = annotationAttributes.getNumber(REQUEST_TIMEOUT);
        int requestTimeout = Integer.MAX_VALUE != requestTimeoutNum.intValue() ? annotationAttributes.getNumber(REQUEST_TIMEOUT)
                : infinityProperties.getConsumer().getRequestTimeout();
        consumerAttributesMap.put(REQUEST_TIMEOUT, requestTimeout);

        consumerAttributesMap.put(DIRECT_URL, annotationAttributes.getString(DIRECT_URL));

        return consumerAttributesMap;
    }

}
