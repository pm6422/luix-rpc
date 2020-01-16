package org.infinity.rpc.server;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.server.annotation.Provider;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
public class ProviderAnnotationBean implements BeanPostProcessor, BeanFactoryPostProcessor {
    public static final Pattern             COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");
    private             ApplicationContext  applicationContext;
    private final       Map<String, Object> rpcProviderMap      = new ConcurrentHashMap<String, Object>();


    public ProviderAnnotationBean(ApplicationContext applicationContext) {
        Assert.notNull(applicationContext, "Application context must not be null!");
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return null;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = getTargetClass(bean);

        getProviderOnClass(bean, clazz);
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

    private void getProviderOnClass(Object bean, Class clazz) {
        Annotation classAnnotation = clazz.getDeclaredAnnotation(Provider.class);
        final Class<?>[] interfaces = bean.getClass().getInterfaces();
        String serviceInterfaceName;
        if (interfaces.length == 1) {
            serviceInterfaceName = interfaces[0].getName();
        } else {
            // Get service interface from annotation if an instance has more than one declared interfaces
            serviceInterfaceName = bean.getClass().getAnnotation(Provider.class).interfaceClass().getName();
        }

        String key = serviceInterfaceName;
        rpcProviderMap.putIfAbsent(key, classAnnotation);
        log.info("Discovered RPC Service provider [{}]", bean.getClass().getName());
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    public Map<String, Object> getRpcProviderMap() {
        return rpcProviderMap;
    }
}
