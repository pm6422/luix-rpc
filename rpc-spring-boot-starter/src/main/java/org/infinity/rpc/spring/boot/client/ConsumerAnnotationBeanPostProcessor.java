package org.infinity.rpc.spring.boot.client;

import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.core.exchange.client.stub.ConsumerStub;
import org.infinity.rpc.spring.boot.bean.processor.AbstractAnnotationBeanPostProcessor;
import org.infinity.rpc.spring.boot.client.stub.ConsumerBeanNameBuilder;
import org.infinity.rpc.spring.boot.client.stub.ConsumerStubBeanNameBuilder;
import org.infinity.rpc.spring.boot.utils.AnnotationUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Objects;

import static org.infinity.rpc.core.constant.ConsumerConstants.GROUP;
import static org.infinity.rpc.core.constant.ConsumerConstants.VERSION;
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
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName,
                                       Class<?> consumerInterfaceClass,
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
        Objects.requireNonNull(beanFactory.getBean(consumerStubBeanName));
        Assert.isTrue(!beanFactory.containsBeanDefinition(consumerStubBeanName), "bean definition exists!");
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
        ConsumerStub<?> consumerStub = new ConsumerStub<>(interfaceClass, annotationAttributes);
        return consumerStub;
    }
}
