package org.infinity.rpc.core.config.spring.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.config.spring.utils.AnnotationUtils;
import org.infinity.rpc.core.registry.RegistryConfig;
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
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;

import static org.infinity.rpc.core.config.spring.utils.AnnotationUtils.getAnnotationAttributes;

/**
 * The class implements {@link BeanPostProcessor} means that
 * all spring bean will be processed by {@link ConsumerBeanPostProcessor#postProcessBeforeInitialization(Object, String)} after initialized bean
 */
@Slf4j
public class ConsumerBeanPostProcessor implements ApplicationContextAware, BeanPostProcessor, BeanFactoryPostProcessor, EnvironmentAware, BeanFactoryAware {
    private String[]                        scanBasePackages;
    private ApplicationContext              applicationContext;
    private Environment                     environment;
    private ConfigurableListableBeanFactory beanFactory;

    public ConsumerBeanPostProcessor(String[] scanBasePackages) {
        Assert.notEmpty(scanBasePackages, "Consumer scan packages must NOT be empty!");
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory, "It requires an instance of ConfigurableListableBeanFactory");
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    /**
     * Inject RPC consumer proxy
     *
     * @param bean     bean instance
     * @param beanName bean name
     * @return processed bean instance
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = getTargetClass(bean);

        if (!matchScanPackages(clazz)) {
            return bean;
        }
        // Field dependency injection by reflection
        setConsumerOnField(bean, clazz);
        // Method dependency injection by reflection
        setConsumerOnMethod(bean, clazz);
        return bean;
    }

    private Class getTargetClass(Object bean) {
        if (isProxyBean(bean)) {
            return AopUtils.getTargetClass(bean);
        }
        return bean.getClass();
    }

    private boolean isProxyBean(Object bean) {
        return AopUtils.isAopProxy(bean);
    }

    private boolean matchScanPackages(Class clazz) {
        return Arrays.asList(scanBasePackages).stream().anyMatch(pkg -> clazz.getName().startsWith(pkg));
    }

    /**
     * Field dependency injection by reflection
     *
     * @param bean      bean instance
     * @param beanClass bean class used to be injected with {@link Consumer} annotated field
     */
    private void setConsumerOnField(Object bean, Class beanClass) {
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                try {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }

                    // Get @Consumer annotation attributes value of field, and it will be null if no annotation presents on the field
                    AnnotationAttributes annotationAttributes = getAnnotationAttributes(field, Consumer.class, environment, false, true);
                    if (annotationAttributes != null) {
                        // Found the @Consumer annotated field
                        Class<?> interfaceClass = AnnotationUtils.resolveInterfaceClass(annotationAttributes, field.getType());
                        // TODO: Register consumer wrapper bean definition
                        // Register consumer wrapper bean to spring context
                        ConsumerWrapper consumerWrapper = registerConsumerWrapper(interfaceClass, annotationAttributes);
                        // Inject consumer proxy instance
                        field.set(bean, consumerWrapper.getProxyInstance());
                    }
                } catch (Throwable t) {
                    throw new BeanInitializationException("Failed to set RPC consumer proxy by field reflection" + field.getName()
                            + " in class " + bean.getClass().getName(), t);
                }
            }
        }
    }

    /**
     * Method dependency injection by reflection
     *
     * @param bean      bean instance
     * @param beanClass bean class used to be injected with {@link Consumer} annotated method
     */
    private void setConsumerOnMethod(Object bean, Class beanClass) {
        Method[] methods = beanClass.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.length() > 3 && methodName.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    // The Java compiler generates the bridge method, in order to be compatible with the byte code under previous JDK version of JDK 1.5,
                    // for the generic erasure occasion
                    Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
                    // Get @Consumer annotation attributes value of method, and it will be null if no annotation presents on the field
                    AnnotationAttributes annotationAttributes = getAnnotationAttributes(bridgedMethod, Consumer.class, environment, false, true);
                    if (annotationAttributes != null) {
                        // Found the @Consumer annotated method
                        Class<?> interfaceClass = AnnotationUtils.resolveInterfaceClass(annotationAttributes, method.getParameterTypes()[0]);
                        // TODO: Register consumer wrapper bean definition
                        // Register consumer wrapper bean to spring context
                        ConsumerWrapper consumerWrapper = registerConsumerWrapper(interfaceClass, annotationAttributes);
                        // Inject consumer proxy instance
                        method.invoke(bean, new Object[]{consumerWrapper.getProxyInstance()});
                    }
                } catch (Throwable t) {
                    throw new BeanInitializationException("Failed to set RPC consumer proxy by setter method " + methodName
                            + " in class " + bean.getClass().getName(), t);
                }
            }
        }
    }

    private <T> ConsumerWrapper registerConsumerWrapper(Class<T> interfaceClass, AnnotationAttributes annotationAttributes) {
        InfinityProperties infinityProperties = applicationContext.getBean(InfinityProperties.class);
        RegistryConfig registryConfig = applicationContext.getBean(RegistryConfig.class);
        // Build the consumer wrapper bean name
        String consumerWrapperBeanName = buildConsumerWrapperBeanName(interfaceClass);

        if (!existsConsumerWrapperBean(consumerWrapperBeanName)) {
            if (!beanFactory.containsBean(consumerWrapperBeanName)) {
                // lombok plugin bug: not support generic
//                ConsumerWrapper consumerWrapper = ConsumerWrapper.builder()
//                        .infinityProperties(infinityProperties)
//                        .registryConfig(registryConfig)
//                        .interfaceClass(interfaceClass)
//                        .instanceName(consumerWrapperBeanName)
//                        .directUrl(annotationAttributes.getString("timeout"))
//                        .build();

                ConsumerWrapper consumerWrapper = new ConsumerWrapper(infinityProperties, registryConfig, interfaceClass,
                        consumerWrapperBeanName, new HashMap<>(annotationAttributes));
                beanFactory.registerSingleton(consumerWrapperBeanName, consumerWrapper);
                return consumerWrapper;
            }
        }

        return null;
    }

    /**
     * Build the consumer wrapper bean name
     *
     * @param defaultInterfaceClass the consumer service interface
     * @return The name of bean that annotated {@link Consumer @Consumer} in spring context
     */
    private String buildConsumerWrapperBeanName(Class<?> defaultInterfaceClass) {
        return ConsumerWrapperBeanNameBuilder.builder(defaultInterfaceClass, environment).build();
    }

    private boolean existsConsumerWrapperBean(String consumerWrapperBeanName) {
        return applicationContext.containsBean(consumerWrapperBeanName);
    }

    /**
     * @param bean     bean instance
     * @param beanName bean name
     * @return bean instance
     * @throws BeansException if any {@link BeansException} thrown
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * @param beanFactory bean factory
     * @throws BeansException if any {@link BeansException} thrown
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // Leave blank intentionally for now
    }
}
