package org.infinity.rpc.core.config.spring.bean.registry;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.NonNull;

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
     *
     * @param beanName       bean name
     * @param beanDefinition bean definition
     * @return true: candidate, false: not candidate
     */
    @Override
    public boolean checkCandidate(@NonNull String beanName, @NonNull BeanDefinition beanDefinition) {
        return super.checkCandidate(beanName, beanDefinition);
    }
}