package org.infinity.rpc.spring.boot.bean;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.spring.boot.bean.name.ConsumerStubBeanNameBuilder;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.spring.boot.utils.AnnotationUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.core.constant.ProtocolConstants.PROTOCOL;
import static org.infinity.rpc.core.constant.ServiceConstants.INTERFACE_CLASS;
import static org.infinity.rpc.spring.boot.utils.AnnotationBeanDefinitionUtils.addPropertyValue;


/**
 * Scan all spring bean to discover the fields and method annotated with {@link Consumer} annotation
 * and injected with the proxyInstance.
 * The class implements {@link BeanPostProcessor} means that all spring beans will be processed by
 * {@link ConsumerBeanPostProcessor#postProcessBeforeInitialization(Object, String)} after initialized bean
 */
@Slf4j
public class ConsumerBeanPostProcessor implements BeanPostProcessor, EnvironmentAware, BeanFactoryAware {
    private final String[]                   scanBasePackages;
    private       Environment                env;
    /**
     * {@link DefaultListableBeanFactory} can register bean definition
     */
    private       DefaultListableBeanFactory beanFactory;

    public ConsumerBeanPostProcessor(String[] scanBasePackages) {
        Assert.notEmpty(scanBasePackages, "Consumer scan packages must NOT be empty!");
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setEnvironment(@NonNull Environment env) {
        this.env = env;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(DefaultListableBeanFactory.class, beanFactory,
                "It requires an instance of ".concat(DefaultListableBeanFactory.class.getSimpleName()));
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    /**
     * Inject RPC consumer proxy and register {@link ConsumerStub} instance
     *
     * @param bean     bean instance to be injected
     * @param beanName bean name to be injected
     * @return processed bean instance
     * @throws BeansException if BeansException throws
     */
    @Override
    public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> clazz = getTargetClass(bean);

        if (!matchScanPackages(clazz)) {
            return bean;
        }

        // Inject consumer proxy instances to fields
        injectConsumerToField(bean, clazz);

        // Inject consumer proxy instances to method parameters
        injectConsumerToMethodParam(bean, clazz);
        return bean;
    }

    private Class<?> getTargetClass(Object bean) {
        if (isProxyBean(bean)) {
            // Get class of the bean if it is a proxy bean
            return AopUtils.getTargetClass(bean);
        }
        return bean.getClass();
    }

    private boolean isProxyBean(Object bean) {
        return AopUtils.isAopProxy(bean);
    }

    private boolean matchScanPackages(Class<?> clazz) {
        return Arrays.stream(scanBasePackages).anyMatch(pkg -> clazz.getName().startsWith(pkg));
    }

    /**
     * Inject RPC consumer proxy instances to fields which annotated with {@link Consumer} by reflection
     * and register its {@link ConsumerStub} instance to spring context
     *
     * @param bean      bean instance to be injected
     * @param beanClass bean class to be injected
     */
    private void injectConsumerToField(Object bean, Class<?> beanClass) {
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            if (!isConsumerAnnotatedField(field)) {
                continue;
            }
            try {
                Consumer consumerAnnotation = field.getAnnotation(Consumer.class);
                if (consumerAnnotation == null) {
                    // No @Consumer annotated field found
                    continue;
                }
                AnnotationAttributes attributes = getConsumerAnnotationAttributes(field);
                // Register consumer stub instance to spring context
                ConsumerStub<?> consumerStub = registerConsumerStub(consumerAnnotation, attributes, field.getType());
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                // Inject RPC consumer proxy instance
                field.set(bean, consumerStub.getProxyInstance());
            } catch (Throwable t) {
                throw new BeanInitializationException("Failed to inject RPC consumer proxy to field [" + field.getName()
                        + "] of " + bean.getClass().getName(), t);
            }
        }
    }

    /**
     * Inject RPC consumer proxy instances to setter method parameters which annotated with {@link Consumer} by reflection
     * and register its {@link ConsumerStub} instance to spring context
     *
     * @param bean      bean instance to be injected
     * @param beanClass bean class to be injected
     */
    private void injectConsumerToMethodParam(Object bean, Class<?> beanClass) {
        Method[] methods = beanClass.getMethods();
        for (Method method : methods) {
            if (!isConsumerAnnotatedMethod(method)) {
                continue;
            }
            try {
                // The Java compiler generates the bridge method, in order to be compatible with the byte code
                // under previous JDK version of JDK 1.5, for the generic erasure occasion
                Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
                Consumer consumerAnnotation = bridgedMethod.getAnnotation(Consumer.class);
                if (consumerAnnotation == null) {
                    // No @Consumer annotated method found
                    continue;
                }

                AnnotationAttributes attributes = getConsumerAnnotationAttributes(bridgedMethod);
                // Register consumer stub instance to spring context
                ConsumerStub<?> consumerStub = registerConsumerStub(consumerAnnotation, attributes, method.getParameterTypes()[0]);
                // Inject RPC consumer proxy instance
                method.invoke(bean, consumerStub.getProxyInstance());
            } catch (Throwable t) {
                throw new BeanInitializationException("Failed to inject RPC consumer proxy to parameter of method ["
                        + method.getName() + "] of " + bean.getClass().getName(), t);
            }
        }
    }

    private boolean isConsumerAnnotatedField(Field field) {
        return !Modifier.isStatic(field.getModifiers())
                && field.isAnnotationPresent(Consumer.class);
    }

    private boolean isConsumerAnnotatedMethod(Method method) {
        return method.getName().startsWith("set")
                && method.getParameterTypes().length == 1
                && Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && method.isAnnotationPresent(Consumer.class);
    }

    private AnnotationAttributes getConsumerAnnotationAttributes(AnnotatedElement element) {
        return AnnotationUtils.getAnnotationAttributes(element, Consumer.class, env, true, true);
    }

    /**
     * Register consumer stub to spring context
     *
     * @param consumerAnnotation     {@link Consumer} annotation
     * @param attributes             {@link AnnotationAttributes annotation attributes}
     * @param consumerInterfaceClass Consumer interface class
     * @return ConsumerStub instance
     */
    private ConsumerStub<?> registerConsumerStub(Consumer consumerAnnotation,
                                                 AnnotationAttributes attributes,
                                                 Class<?> consumerInterfaceClass) {
        // Resolve the interface class of the consumer proxy instance
        Class<?> resolvedConsumerInterfaceClass = AnnotationUtils.resolveInterfaceClass(attributes, consumerInterfaceClass);

        // Build the consumer stub bean name
        String consumerStubBeanName = buildConsumerStubBeanName(resolvedConsumerInterfaceClass, attributes);
        if (!existsConsumerStub(consumerStubBeanName)) {
            AbstractBeanDefinition stubBeanDefinition = buildConsumerStubDefinition(consumerStubBeanName, consumerInterfaceClass, consumerAnnotation);
            beanFactory.registerBeanDefinition(consumerStubBeanName, stubBeanDefinition);
            log.info("Registered RPC consumer stub [{}] to spring context", consumerStubBeanName);
        }
        // getBean() will trigger bean initialization
        return beanFactory.getBean(consumerStubBeanName, ConsumerStub.class);
    }

    /**
     * Build the consumer stub bean name
     *
     * @param defaultInterfaceClass the consumer service interface
     * @param attributes            {@link AnnotationAttributes annotation attributes}
     * @return The name of bean that annotated {@link Consumer @Consumer} in spring context
     */
    private String buildConsumerStubBeanName(Class<?> defaultInterfaceClass, AnnotationAttributes attributes) {
        return ConsumerStubBeanNameBuilder
                .builder(defaultInterfaceClass.getName(), env)
                .attributes(attributes)
                .build();
    }

    private boolean existsConsumerStub(String consumerStubBeanName) {
        return beanFactory.containsBeanDefinition(consumerStubBeanName);
    }

    /**
     * Build {@link ConsumerStub} definition
     *
     * @param beanName consumer stub bean name
     * @param interfaceClass consumer interface class
     * @param annotation     {@link Consumer} annotation
     * @return {@link ConsumerStub} bean definition
     */
    private AbstractBeanDefinition buildConsumerStubDefinition(String beanName,
                                                               Class<?> interfaceClass,
                                                               Consumer annotation) {
        // Create and load infinityProperties bean
        InfinityProperties infinityProperties = beanFactory.getBean(InfinityProperties.class);
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ConsumerStub.class);

        addPropertyValue(builder, BEAN_NAME, beanName);
        addPropertyValue(builder, INTERFACE_CLASS, interfaceClass);
        addPropertyValue(builder, INTERFACE_NAME, interfaceClass.getName());

        if (StringUtils.isEmpty(annotation.protocol())) {
            addPropertyValue(builder, PROTOCOL, infinityProperties.getProtocol().getName());
        } else {
            addPropertyValue(builder, PROTOCOL, annotation.protocol());
        }
        if (StringUtils.isEmpty(annotation.cluster())) {
            addPropertyValue(builder, CLUSTER, infinityProperties.getConsumer().getCluster());
        } else {
            addPropertyValue(builder, CLUSTER, annotation.cluster());
        }
        if (StringUtils.isEmpty(annotation.faultTolerance())) {
            addPropertyValue(builder, FAULT_TOLERANCE, infinityProperties.getConsumer().getFaultTolerance());
        } else {
            addPropertyValue(builder, FAULT_TOLERANCE, annotation.faultTolerance());
        }
        if (StringUtils.isEmpty(annotation.loadBalancer())) {
            addPropertyValue(builder, LOAD_BALANCER, infinityProperties.getConsumer().getLoadBalancer());
        } else {
            addPropertyValue(builder, LOAD_BALANCER, annotation.loadBalancer());
        }
        if (StringUtils.isEmpty(annotation.group())) {
            addPropertyValue(builder, GROUP, infinityProperties.getConsumer().getGroup());
        } else {
            addPropertyValue(builder, GROUP, annotation.group());
        }
        if (StringUtils.isEmpty(annotation.version())) {
            addPropertyValue(builder, VERSION, infinityProperties.getConsumer().getVersion());
        } else {
            addPropertyValue(builder, VERSION, annotation.version());
        }
        if (StringUtils.isEmpty(annotation.proxyFactory())) {
            addPropertyValue(builder, PROXY_FACTORY, infinityProperties.getConsumer().getProxyFactory());
        } else {
            addPropertyValue(builder, PROXY_FACTORY, annotation.proxyFactory());
        }
        if (StringUtils.isEmpty(annotation.checkHealthFactory())) {
            addPropertyValue(builder, CHECK_HEALTH_FACTORY, infinityProperties.getConsumer().getCheckHealthFactory());
        } else {
            addPropertyValue(builder, CHECK_HEALTH_FACTORY, annotation.checkHealthFactory());
        }
        if (Integer.MAX_VALUE == annotation.requestTimeout()) {
            addPropertyValue(builder, REQUEST_TIMEOUT, infinityProperties.getConsumer().getRequestTimeout());
        } else {
            addPropertyValue(builder, REQUEST_TIMEOUT, annotation.requestTimeout());
        }
        if (Integer.MAX_VALUE == annotation.maxRetries()) {
            addPropertyValue(builder, MAX_RETRIES, infinityProperties.getConsumer().getMaxRetries());
        } else {
            addPropertyValue(builder, MAX_RETRIES, annotation.maxRetries());
        }

        addPropertyValue(builder, MAX_PAYLOAD, infinityProperties.getConsumer().getMaxPayload());

        addPropertyValue(builder, DIRECT_ADDRESSES, annotation.directAddresses());

        return builder.getBeanDefinition();
    }

    /**
     * @param bean     bean instance
     * @param beanName bean name
     * @return bean instance
     * @throws BeansException if any {@link BeansException} thrown
     */
    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        return bean;
    }
}
