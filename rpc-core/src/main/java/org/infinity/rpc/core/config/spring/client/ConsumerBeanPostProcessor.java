package org.infinity.rpc.core.config.spring.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.config.spring.utils.AnnotationUtils;
import org.infinity.rpc.core.registry.RegistryInfo;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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
import java.util.HashMap;


/**
 * The class implements {@link BeanPostProcessor} means that all spring beans will be processed by
 * {@link ConsumerBeanPostProcessor#postProcessBeforeInitialization(Object, String)} after initialized bean
 */
@Slf4j
public class ConsumerBeanPostProcessor implements ApplicationContextAware,
        BeanPostProcessor, BeanFactoryPostProcessor, EnvironmentAware, BeanFactoryAware {
    private final String[]                        scanBasePackages;
    private       ApplicationContext              applicationContext;
    private       Environment                     env;
    private       ConfigurableListableBeanFactory beanFactory;

    public ConsumerBeanPostProcessor(String[] scanBasePackages) {
        Assert.notEmpty(scanBasePackages, "Consumer scan packages must NOT be empty!");
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(@NonNull Environment env) {
        this.env = env;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
                "It requires an instance of ".concat(ConfigurableListableBeanFactory.class.getSimpleName()));
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
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
     * Inject RPC consumer proxy and register {@link ConsumerWrapper} instance
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
     * and register its {@link ConsumerWrapper} instance to spring context
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
                // TODO: Register consumer wrapper bean definition
                // Register consumer wrapper instance to spring context
                ConsumerWrapper<?> consumerWrapper = registerConsumerWrapper(field.getType(), attributes);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                // Inject RPC consumer proxy instance
                field.set(bean, consumerWrapper.getProxyInstance());
            } catch (Throwable t) {
                throw new BeanInitializationException("Failed to inject RPC consumer proxy to field [" + field.getName()
                        + "] of " + bean.getClass().getName(), t);
            }
        }
    }

    /**
     * Inject RPC consumer proxy instances to setter method parameters which annotated with {@link Consumer} by reflection
     * and register its {@link ConsumerWrapper} instance to spring context
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
                // TODO: Register consumer wrapper bean definition
                // Register consumer wrapper instance to spring context
                ConsumerWrapper<?> consumerWrapper = registerConsumerWrapper(method.getParameterTypes()[0], attributes);
                // Inject RPC consumer proxy instance
                method.invoke(bean, consumerWrapper.getProxyInstance());
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

    private ConsumerWrapper<?> registerConsumerWrapper(Class<?> consumerType, AnnotationAttributes annotationAttributes) {
        // Resolve the interface class of the consumer proxy instance
        Class<?> consumerInterfaceClass = AnnotationUtils.resolveInterfaceClass(annotationAttributes, consumerType);

        // Build the consumer wrapper bean name
        String consumerWrapperBeanName = buildConsumerWrapperBeanName(consumerInterfaceClass);

        if (registeredConsumerWrapper(consumerWrapperBeanName)) {
            // Return the instance if it already be registered
            return applicationContext.getBean(consumerWrapperBeanName, ConsumerWrapper.class);
        }

        ConsumerWrapper<?> consumerWrapper = createConsumerWrapper(consumerWrapperBeanName, consumerInterfaceClass, annotationAttributes);
        // Register the consumer wrapper instance with singleton scope
        beanFactory.registerSingleton(consumerWrapperBeanName, consumerWrapper);
        return consumerWrapper;
    }

    /**
     * Build the consumer wrapper bean name
     *
     * @param defaultInterfaceClass the consumer service interface
     * @return The name of bean that annotated {@link Consumer @Consumer} in spring context
     */
    private String buildConsumerWrapperBeanName(Class<?> defaultInterfaceClass) {
        return ConsumerWrapperBeanNameBuilder.builder(defaultInterfaceClass, env).build();
    }

    private boolean registeredConsumerWrapper(String consumerWrapperBeanName) {
        return applicationContext.containsBean(consumerWrapperBeanName);
    }

    private ConsumerWrapper<?> createConsumerWrapper(String consumerWrapperBeanName,
                                                     Class<?> consumerInterfaceClass,
                                                     AnnotationAttributes annotationAttributes) {
        InfinityProperties infinityProperties = applicationContext.getBean(InfinityProperties.class);
        RegistryInfo registryInfo = applicationContext.getBean(RegistryInfo.class);
        ConsumerWrapper<?> consumerWrapper = new ConsumerWrapper<>(consumerWrapperBeanName, consumerInterfaceClass);
        consumerWrapper.init(infinityProperties, registryInfo.getRegistryUrls(), new HashMap<>(annotationAttributes));
        return consumerWrapper;
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
