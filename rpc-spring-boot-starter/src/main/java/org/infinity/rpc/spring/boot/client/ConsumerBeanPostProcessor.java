package org.infinity.rpc.spring.boot.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.core.constant.BooleanEnum;
import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.exchange.client.stub.ConsumerStub;
import org.infinity.rpc.spring.boot.client.stub.ConsumerStubBeanNameBuilder;
import org.infinity.rpc.spring.boot.utils.AnnotationUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
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
import static org.infinity.rpc.core.constant.ServiceConstants.PROTOCOL;
import static org.infinity.rpc.core.constant.ServiceConstants.REGISTRY;


/**
 * The class implements {@link BeanPostProcessor} means that all spring beans will be processed by
 * {@link ConsumerBeanPostProcessor#postProcessBeforeInitialization(Object, String)} after initialized bean
 */
@Slf4j
public class ConsumerBeanPostProcessor implements BeanPostProcessor, BeanFactoryPostProcessor, EnvironmentAware, BeanFactoryAware {
    private static final ValidatorFactory           VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final        String[]                   scanBasePackages;
    private              Environment                env;
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
     * @param beanFactory bean factory
     * @throws BeansException if any {@link BeansException} thrown
     */
    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // Leave blank intentionally for now
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
                // Get @Consumer annotation attribute values of field, and it will be null if no annotation presents on the field
                AnnotationAttributes attributes = getConsumerAnnotationAttributes(field);
                if (attributes == null) {
                    // No @Consumer annotated field found
                    continue;
                }
                // TODO: Register consumer stub bean definition
                // Register consumer stub instance to spring context
                ConsumerStub<?> consumerStub = registerConsumerStub(attributes, field.getType());
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
                // Get @Consumer annotation attribute values of method, and it will be null if no annotation presents on the field
                AnnotationAttributes attributes = getConsumerAnnotationAttributes(bridgedMethod);
                if (attributes == null) {
                    // No method with @Consumer annotated parameter found
                    continue;
                }
                // Register consumer stub instance to spring context
                ConsumerStub<?> consumerStub = registerConsumerStub(attributes, method.getParameterTypes()[0]);
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
     * @param attributes             {@link AnnotationAttributes annotation attributes}
     * @param consumerInterfaceClass Consumer interface class
     * @return ConsumerStub instance
     */
    private ConsumerStub<?> registerConsumerStub(AnnotationAttributes attributes, Class<?> consumerInterfaceClass) {
        // Resolve the interface class of the consumer proxy instance
        Class<?> resolvedConsumerInterfaceClass = AnnotationUtils.resolveInterfaceClass(attributes, consumerInterfaceClass);

        // Build the consumer stub bean name
        String beanName = buildConsumerStubBeanName(resolvedConsumerInterfaceClass, attributes);

        if (existsConsumerStub(beanName)) {
            // Return the instance if it already be registered
            return beanFactory.getBean(beanName, ConsumerStub.class);
        }

        AbstractBeanDefinition stubBeanDefinition = buildConsumerStubDefinition(consumerInterfaceClass, attributes);
        beanFactory.registerBeanDefinition(beanName, stubBeanDefinition);
        return beanFactory.getBean(beanName, ConsumerStub.class);
    }

    /**
     * Build the consumer stub bean name
     *
     * @param defaultInterfaceClass the consumer service interface
     * @return The name of bean that annotated {@link Consumer @Consumer} in spring context
     */
    private String buildConsumerStubBeanName(Class<?> defaultInterfaceClass, AnnotationAttributes attributes) {
        return ConsumerStubBeanNameBuilder
                .builder(defaultInterfaceClass.getName(), env)
                .group(attributes.getString(GROUP))
                .version(attributes.getString(VERSION))
                .build();
    }

    private boolean existsConsumerStub(String consumerStubBeanName) {
        return beanFactory.containsBean(consumerStubBeanName);
    }

    /**
     * Build {@link ConsumerStub} definition
     *
     * @param consumerInterfaceClass consumer interface class
     * @param attributes             {@link AnnotationAttributes annotation attributes}
     * @return {@link ConsumerStub} bean definition
     */
    private AbstractBeanDefinition buildConsumerStubDefinition(Class<?> consumerInterfaceClass,
                                                               AnnotationAttributes attributes) {

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ConsumerStub.class);
        addPropertyValue(builder, "interfaceName", consumerInterfaceClass.getName(), true);
        addPropertyValue(builder, "interfaceClass", consumerInterfaceClass, true);
        addPropertyValue(builder, "registry", attributes.getString(REGISTRY), false);
        addPropertyValue(builder, "protocol", attributes.getString(PROTOCOL), false);
        addPropertyValue(builder, "cluster", attributes.getString(CLUSTER), false);
        addPropertyValue(builder, "faultTolerance", attributes.getString(FAULT_TOLERANCE), false);
        addPropertyValue(builder, "loadBalancer", attributes.getString(LOAD_BALANCER), false);
        addPropertyValue(builder, "group", attributes.getString(GROUP), false);
        addPropertyValue(builder, "version", attributes.getString(VERSION), false);

        BooleanEnum checkHealthEnum = attributes.getEnum(CHECK_HEALTH);
        addPropertyValue(builder, "checkHealth", checkHealthEnum.getValue(), false);

        addPropertyValue(builder, "checkHealthFactory", attributes.getString(CHECK_HEALTH_FACTORY), false);
        addPropertyValue(builder, "requestTimeout", attributes.getNumber(REQUEST_TIMEOUT).intValue(), false);
        addPropertyValue(builder, "directUrl", attributes.getString(DIRECT_URL), false);
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
