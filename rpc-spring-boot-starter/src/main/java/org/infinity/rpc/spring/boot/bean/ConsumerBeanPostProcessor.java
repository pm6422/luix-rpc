package org.infinity.rpc.spring.boot.bean;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.annotation.RpcConsumer;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.spring.boot.config.InfinityProperties;
import org.infinity.rpc.spring.boot.utils.AnnotationUtils;
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

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.infinity.rpc.core.client.stub.ConsumerStub.buildConsumerStubBeanName;
import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.core.constant.ProtocolConstants.PROTOCOL;
import static org.infinity.rpc.core.constant.ServiceConstants.INTERFACE_CLASS;
import static org.infinity.rpc.spring.boot.utils.AnnotationBeanDefinitionUtils.addPropertyValue;
import static org.infinity.rpc.spring.boot.utils.ProxyUtils.getTargetClass;


/**
 * Scan all spring bean to discover the fields and method annotated with {@link RpcConsumer} annotation
 * and injected with the proxyInstance.
 * The class implements {@link BeanPostProcessor} means that all spring beans will be processed by
 * {@link ConsumerBeanPostProcessor#postProcessBeforeInitialization(Object, String)} after initialized bean
 * <p>
 * BeanPostProcessor: Factory hook that allows for custom modification of new bean instances —
 * for example, checking for marker interfaces or wrapping beans with proxies.
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

    private boolean matchScanPackages(Class<?> clazz) {
        return Arrays.stream(scanBasePackages).anyMatch(pkg -> clazz.getName().startsWith(pkg));
    }

    /**
     * Inject RPC consumer proxy instances to fields which annotated with {@link RpcConsumer} by reflection
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
                RpcConsumer rpcConsumerAnnotation = field.getAnnotation(RpcConsumer.class);
                if (rpcConsumerAnnotation == null) {
                    // No @Consumer annotated field found
                    continue;
                }
                AnnotationAttributes attributes = getConsumerAnnotationAttributes(field);
                // Register consumer stub instance to spring context
                ConsumerStub<?> consumerStub = registerConsumerStub(rpcConsumerAnnotation, attributes, field.getType());
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
     * Inject RPC consumer proxy instances to setter method parameters which annotated with {@link RpcConsumer} by reflection
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
                RpcConsumer rpcConsumerAnnotation = bridgedMethod.getAnnotation(RpcConsumer.class);
                if (rpcConsumerAnnotation == null) {
                    // No @Consumer annotated method found
                    continue;
                }

                AnnotationAttributes attributes = getConsumerAnnotationAttributes(bridgedMethod);
                // Register consumer stub instance to spring context
                ConsumerStub<?> consumerStub = registerConsumerStub(rpcConsumerAnnotation, attributes, method.getParameterTypes()[0]);
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
                && field.isAnnotationPresent(RpcConsumer.class);
    }

    private boolean isConsumerAnnotatedMethod(Method method) {
        return method.getName().startsWith("set")
                && method.getParameterTypes().length == 1
                && Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && method.isAnnotationPresent(RpcConsumer.class);
    }

    private AnnotationAttributes getConsumerAnnotationAttributes(AnnotatedElement element) {
        return AnnotationUtils.getAnnotationAttributes(element, RpcConsumer.class, env, true, true);
    }

    /**
     * Register consumer stub to spring context
     *
     * @param rpcConsumerAnnotation     {@link RpcConsumer} annotation
     * @param attributes             {@link AnnotationAttributes annotation attributes}
     * @param consumerInterfaceClass Consumer interface class
     * @return ConsumerStub instance
     */
    private ConsumerStub<?> registerConsumerStub(RpcConsumer rpcConsumerAnnotation,
                                                 AnnotationAttributes attributes,
                                                 Class<?> consumerInterfaceClass) {
        // Resolve the interface class of the consumer proxy instance
        Class<?> resolvedConsumerInterfaceClass = AnnotationUtils.resolveInterfaceClass(attributes, consumerInterfaceClass);

        // Build the consumer stub bean name
        String consumerStubBeanName = buildConsumerStubBeanName(resolvedConsumerInterfaceClass.getName(), attributes);
        if (!existsConsumerStub(consumerStubBeanName)) {
            AbstractBeanDefinition stubBeanDefinition = buildConsumerStubDefinition(consumerStubBeanName, consumerInterfaceClass, rpcConsumerAnnotation);
            beanFactory.registerBeanDefinition(consumerStubBeanName, stubBeanDefinition);
            log.info("Registered RPC consumer stub [{}] to spring context", consumerStubBeanName);
        }
        // Method getBean() will trigger bean initialization
        return beanFactory.getBean(consumerStubBeanName, ConsumerStub.class);
    }

    private boolean existsConsumerStub(String consumerStubBeanName) {
        return beanFactory.containsBeanDefinition(consumerStubBeanName);
    }

    /**
     * Build {@link ConsumerStub} definition
     *
     * @param beanName       consumer stub bean name
     * @param interfaceClass consumer interface class
     * @param annotation     {@link RpcConsumer} annotation
     * @return {@link ConsumerStub} bean definition
     */
    private AbstractBeanDefinition buildConsumerStubDefinition(String beanName,
                                                               Class<?> interfaceClass,
                                                               RpcConsumer annotation) {
        // Create and load infinityProperties bean
        InfinityProperties infinityProperties = beanFactory.getBean(InfinityProperties.class);
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ConsumerStub.class);

        addPropertyValue(builder, BEAN_NAME, beanName);
        addPropertyValue(builder, INTERFACE_CLASS, interfaceClass);
        addPropertyValue(builder, INTERFACE_NAME, interfaceClass.getName());

        String protocol = defaultIfEmpty(annotation.protocol(), infinityProperties.getProtocol().getName());
        addPropertyValue(builder, PROTOCOL, protocol);

        String invoker = defaultIfEmpty(annotation.invoker(), infinityProperties.getConsumer().getInvoker());
        addPropertyValue(builder, INVOKER, invoker);

        String faultTolerance = defaultIfEmpty(annotation.faultTolerance(), infinityProperties.getConsumer().getFaultTolerance());
        addPropertyValue(builder, FAULT_TOLERANCE, faultTolerance);

        String loadBalancer = defaultIfEmpty(annotation.loadBalancer(), infinityProperties.getConsumer().getLoadBalancer());
        addPropertyValue(builder, LOAD_BALANCER, loadBalancer);

        String form = defaultIfEmpty(annotation.form(), infinityProperties.getConsumer().getForm());
        addPropertyValue(builder, FORM, form);

        String version = defaultIfEmpty(annotation.version(), infinityProperties.getConsumer().getVersion());
        addPropertyValue(builder, VERSION, version);

        String proxyFactory = defaultIfEmpty(annotation.proxyFactory(), infinityProperties.getConsumer().getProxyFactory());
        addPropertyValue(builder, PROXY, proxyFactory);

        Integer requestTimeout = StringUtils.isEmpty(annotation.requestTimeout())
                ? infinityProperties.getConsumer().getRequestTimeout() : Integer.valueOf(annotation.requestTimeout());
        addPropertyValue(builder, REQUEST_TIMEOUT, requestTimeout);

        Integer maxRetries = StringUtils.isEmpty(annotation.maxRetries())
                ? infinityProperties.getConsumer().getMaxRetries() : Integer.valueOf(annotation.maxRetries());
        addPropertyValue(builder, MAX_RETRIES, maxRetries);

        addPropertyValue(builder, LIMIT_RATE, infinityProperties.getConsumer().isLimitRate());
        addPropertyValue(builder, MAX_PAYLOAD, infinityProperties.getConsumer().getMaxPayload());
        addPropertyValue(builder, PROVIDER_ADDRESSES, annotation.providerAddresses());

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
