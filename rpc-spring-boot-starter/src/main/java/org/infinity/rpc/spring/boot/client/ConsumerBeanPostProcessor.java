package org.infinity.rpc.spring.boot.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.exchange.client.stub.ConsumerStub;
import org.infinity.rpc.spring.boot.client.stub.ConsumerStubBeanNameBuilder;
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

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;


/**
 * The class implements {@link BeanPostProcessor} means that all spring beans will be processed by
 * {@link ConsumerBeanPostProcessor#postProcessBeforeInitialization(Object, String)} after initialized bean
 */
@Slf4j
public class ConsumerBeanPostProcessor implements BeanPostProcessor, EnvironmentAware, BeanFactoryAware {
    private static final ValidatorFactory           VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final        String[]                   scanBasePackages;
    private              Environment                env;
    /**
     * {@link DefaultListableBeanFactory} can register bean definition
     */
    private              DefaultListableBeanFactory beanFactory;

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
        return AnnotationUtils.getAnnotationAttributes(element, Consumer.class, env, false, true);
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
        String beanName = buildConsumerStubBeanName(resolvedConsumerInterfaceClass, consumerAnnotation);
        if (!existsConsumerStub(beanName)) {
            AbstractBeanDefinition stubBeanDefinition = buildConsumerStubDefinition(consumerInterfaceClass, consumerAnnotation);
            beanFactory.registerBeanDefinition(beanName, stubBeanDefinition);
        }
        // getBean() will trigger bean initialization
        return beanFactory.getBean(beanName, ConsumerStub.class);
    }

    /**
     * Build the consumer stub bean name
     *
     * @param defaultInterfaceClass the consumer service interface
     * @param consumerAnnotation    {@link Consumer} annotation
     * @return The name of bean that annotated {@link Consumer @Consumer} in spring context
     */
    private String buildConsumerStubBeanName(Class<?> defaultInterfaceClass, Consumer consumerAnnotation) {
        return ConsumerStubBeanNameBuilder
                .builder(defaultInterfaceClass.getName(), env)
                .group(consumerAnnotation.group())
                .version(consumerAnnotation.version())
                .build();
    }

    private boolean existsConsumerStub(String consumerStubBeanName) {
        return beanFactory.containsBeanDefinition(consumerStubBeanName);
    }

    /**
     * Build {@link ConsumerStub} definition
     *
     * @param consumerInterfaceClass consumer interface class
     * @param consumerAnnotation     {@link Consumer} annotation
     * @return {@link ConsumerStub} bean definition
     */
    private AbstractBeanDefinition buildConsumerStubDefinition(Class<?> consumerInterfaceClass,
                                                               Consumer consumerAnnotation) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ConsumerStub.class);
        addPropertyValue(builder, INTERFACE_NAME, consumerInterfaceClass.getName(), true);
        addPropertyValue(builder, INTERFACE_CLASS, consumerInterfaceClass, true);
        addPropertyValue(builder, REGISTRY, consumerAnnotation.registry(), false);
        addPropertyValue(builder, PROTOCOL, consumerAnnotation.protocol(), false);
        addPropertyValue(builder, CLUSTER, consumerAnnotation.cluster(), false);
        addPropertyValue(builder, FAULT_TOLERANCE, consumerAnnotation.faultTolerance(), false);
        addPropertyValue(builder, LOAD_BALANCER, consumerAnnotation.loadBalancer(), false);
        addPropertyValue(builder, GROUP, consumerAnnotation.group(), false);
        addPropertyValue(builder, VERSION, consumerAnnotation.version(), false);
        addPropertyValue(builder, CHECK_HEALTH, consumerAnnotation.checkHealth().getValue(), false);
        addPropertyValue(builder, CHECK_HEALTH_FACTORY, consumerAnnotation.checkHealthFactory(), false);
        addPropertyValue(builder, REQUEST_TIMEOUT, consumerAnnotation.requestTimeout(), false);
        addPropertyValue(builder, MAX_RETRIES, consumerAnnotation.maxRetries(), true);

        addPropertyValue(builder, "directUrl", consumerAnnotation.directUrl(), false);
        return builder.getBeanDefinition();
    }

    private void addPropertyValue(BeanDefinitionBuilder builder, String propertyName, Object propertyValue, boolean validate) {
        if (validate) {
            validatePropertyValue(builder.getBeanDefinition().getBeanClass(), propertyName, propertyValue);
        }
        builder.addPropertyValue(propertyName, propertyValue);
    }

    private void validatePropertyValue(Class<?> beanType, String propertyName, Object propertyValue) {
        try {
            List<String> messages = doValidate(beanType, propertyName, propertyValue);
            Assert.isTrue(CollectionUtils.isEmpty(messages), String.join(",", messages));
        } catch (Exception e) {
            // Re-throw the exception
            throw new RpcConfigurationException(e.getMessage());
        }
    }

    private static <T> List<String> doValidate(Class<T> beanType, String propertyName, Object propertyValue) {
        Set<ConstraintViolation<T>> constraintViolations = VALIDATOR_FACTORY.getValidator()
                .validateValue(beanType, propertyName, propertyValue);
        return constraintViolations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
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
