package org.infinity.rpc.spring.boot.client;

import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.spring.boot.bean.processor.AbstractAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;

import static org.infinity.rpc.spring.boot.utils.AnnotationUtils.getResolvedAttributes;

@Deprecated
public class ConsumerAnnotationBeanPostProcessor extends AbstractAnnotationBeanPostProcessor {
    /**
     * The bean name of {@link ConsumerAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "consumerAnnotationBeanPostProcessor";

    public ConsumerAnnotationBeanPostProcessor() {
        super(Consumer.class);
    }

    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
                                                 Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        return buildConsumerBeanName(attributes, injectedType) +
                "#source=" + (injectedElement.getMember()) +
                "#attributes=" + getResolvedAttributes(attributes, getEnvironment());
    }

    /**
     * @param attributes            the attributes of {@link org.infinity.rpc.core.client.annotation.Consumer @Consumer}
     * @param consumerInterfaceType the type of consumer interface
     * @return The name of bean that annotated Dubbo's {@link org.infinity.rpc.core.client.annotation.Consumer @Consumer} in local Spring {@link ApplicationContext}
     */
    private String buildConsumerBeanName(AnnotationAttributes attributes, Class<?> consumerInterfaceType) {
        // TODO: refactor
        return consumerInterfaceType.getClass().getSimpleName();
    }

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {
        String referencedBeanName = buildConsumerBeanName(attributes, injectedType);

        return null;
    }
}
