package org.infinity.rpc.core.config.spring.bean.registry;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import static org.springframework.context.annotation.AnnotationConfigUtils.registerAnnotationConfigProcessors;

public class ClassPathBeanDefinitionRegistryScanner extends ClassPathBeanDefinitionScanner {
    public ClassPathBeanDefinitionRegistryScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters, Environment environment, ResourceLoader resourceLoader) {
        super(registry, useDefaultFilters);
        setEnvironment(environment);
        setResourceLoader(resourceLoader);
        registerAnnotationConfigProcessors(registry);
    }

    public ClassPathBeanDefinitionRegistryScanner(BeanDefinitionRegistry registry, Environment environment, ResourceLoader resourceLoader) {
        this(registry, false, environment, resourceLoader);
    }

    /**
     * Expose method to public
     * @param basePackages
     * @return
     */
//    @Override
//    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
//        return super.doScan(basePackages);
//    }

    /**
     * Expose method to public
     *
     * @param beanName
     * @param beanDefinition
     * @return
     */
    @Override
    public boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
        return super.checkCandidate(beanName, beanDefinition);
    }
}