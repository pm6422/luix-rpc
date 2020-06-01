package org.infinity.rpc.core.config.spring.client;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.RpcConsumerFactoryBean;
import org.infinity.rpc.core.client.annotation.Consumer;
import org.infinity.rpc.core.client.proxy.RpcConsumerProxy;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConsumerBeanPostProcessor implements ApplicationContextAware, BeanPostProcessor, BeanFactoryPostProcessor {
    private             ApplicationContext                  applicationContext;
    private             String[]                            scanBasePackages;
    // Consumers are not injected into bean factory, they are saved in this map.
    private final       Map<String, RpcConsumerFactoryBean> rpcConsumerFactoryBeanMap = new ConcurrentHashMap<>();


    public ConsumerBeanPostProcessor(String[] scanBasePackages) {
        Assert.notEmpty(scanBasePackages, "Consumer scan packages must NOT be empty!");
        this.scanBasePackages = scanBasePackages;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Inject RPC consumer proxy
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = getTargetClass(bean);

        if (!matchScanPackages(clazz)) {
            return bean;
        }
        // Activate bean initialization
        RpcConsumerProxy rpcConsumerProxy = applicationContext.getBean(RpcConsumerProxy.class);
        // Method dependency injection by reflection
        setConsumerOnMethod(bean, clazz, rpcConsumerProxy);
        // Field dependency injection by reflection
        setConsumerOnField(bean, clazz, rpcConsumerProxy);
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
     * Method dependency injection by reflection
     *
     * @param bean
     * @param clazz
     * @param rpcConsumerProxy
     */
    private void setConsumerOnMethod(Object bean, Class clazz, RpcConsumerProxy rpcConsumerProxy) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.length() > 3 && methodName.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    Consumer methodAnnotation = method.getAnnotation(Consumer.class);
                    if (methodAnnotation != null) {
                        // if setter method annotated with the @Consumer
                        Object value = getConsumerProxy(methodAnnotation, method.getParameterTypes()[0], rpcConsumerProxy);
                        if (value != null) {
                            method.invoke(bean, new Object[]{value});
                        }
                    }
                } catch (Throwable t) {
                    throw new BeanInitializationException("Failed to set RPC consumer proxy by setter method " + methodName
                            + " in class " + bean.getClass().getName(), t);
                }
            }
        }
    }

    /**
     * Field dependency injection by reflection
     *
     * @param bean
     * @param clazz
     * @param rpcConsumerProxy
     */
    private void setConsumerOnField(Object bean, Class clazz, RpcConsumerProxy rpcConsumerProxy) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                Consumer fieldAnnotation = field.getAnnotation(Consumer.class);
                if (fieldAnnotation != null) {
                    Object value = getConsumerProxy(fieldAnnotation, field.getType(), rpcConsumerProxy);
                    if (value != null) {
                        // if field annotated with the @Consumer
                        field.set(bean, value);
                    }
                }
            } catch (Throwable t) {
                throw new BeanInitializationException("Failed to set RPC consumer proxy by field reflection" + field.getName()
                        + " in class " + bean.getClass().getName(), t);
            }
        }
    }

    private <T> Object getConsumerProxy(Consumer consumer, Class<T> consumerClass, RpcConsumerProxy rpcConsumerProxy) throws Exception {
        String interfaceName;
        Class<T> consumerInterface;
        if (!void.class.equals(consumer.interfaceClass())) {
            interfaceName = consumer.interfaceClass().getName();
            consumerInterface = (Class<T>) consumer.interfaceClass();
        } else if (consumerClass.isInterface()) {
            interfaceName = consumerClass.getName();
            consumerInterface = consumerClass;
        } else {
            throw new IllegalStateException("The consumer must be declared as an interface " +
                    "or specify the interfaceClass attribute value of @Consumer annotation!");
        }

        String key = interfaceName;
        RpcConsumerFactoryBean rpcConsumerFactoryBean = rpcConsumerFactoryBeanMap.get(key);
        if (rpcConsumerFactoryBean == null) {
            rpcConsumerFactoryBean = new RpcConsumerFactoryBean<T>();
            rpcConsumerFactoryBeanMap.putIfAbsent(key, rpcConsumerFactoryBean);
        }
        return rpcConsumerFactoryBean.getObject(rpcConsumerProxy, consumerInterface);
    }

    /**
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // Leave blank intentionally
    }
}
