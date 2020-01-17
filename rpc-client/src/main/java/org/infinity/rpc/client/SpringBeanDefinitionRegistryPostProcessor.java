package org.infinity.rpc.client;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;

public class SpringBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory arg) throws BeansException {

    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
//        registerBean(registry, ClassUtils.getShortNameAsProperty(ConsumerAnnotationBean.class), ConsumerAnnotationBean.class);
    }

    private void registerBean(BeanDefinitionRegistry registry, String name, Class<?> beanClass) {
        RootBeanDefinition bean = new RootBeanDefinition(beanClass);
        registry.registerBeanDefinition(name, bean);
    }
}