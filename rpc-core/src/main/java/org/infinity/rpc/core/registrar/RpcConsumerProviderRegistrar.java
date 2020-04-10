package org.infinity.rpc.core.registrar;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.annotation.EnableRpc;
import org.infinity.rpc.core.client.ConsumerBeanPostProcessor;
import org.infinity.rpc.core.server.ProviderBeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

@Slf4j
public class RpcConsumerProviderRegistrar implements BeanClassLoaderAware, ImportBeanDefinitionRegistrar {

    private ClassLoader classLoader;

    public RpcConsumerProviderRegistrar() {
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Set<String> scanBasePackages = getScanBasePackages(importingClassMetadata);
        registerProviderDefinitionRegistryPostProcessor(scanBasePackages, registry);
        registerConsumerDefinitionRegistryPostProcessor(scanBasePackages, registry);
    }

    private Set<String> getScanBasePackages(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableRpc.class.getName()));
        String[] scanBasePackages = attributes.getStringArray("scanBasePackages");
//        Class<?> currentClass = ClassUtils.resolveClassName(metadata.getClassName(), classLoader);
        Set<String> packagesToScan = new LinkedHashSet<String>(Arrays.asList(scanBasePackages));
        if (packagesToScan.isEmpty()) {
            String packageName = ClassUtils.getPackageName(metadata.getClassName());
            log.debug("Default scan base package: [{}]", packageName);
            Assert.hasText(packageName, "Consumer and provider scan base packages must not be empty!");
            return Collections.singleton(packageName);
        } else {
            log.debug("User defined scan base packages: [{}]", packagesToScan);
        }
        return packagesToScan;
    }

    private void registerProviderDefinitionRegistryPostProcessor(Set<String> scanBasePackages, BeanDefinitionRegistry registry) {
        registerBeanDefinition(scanBasePackages, registry, ProviderBeanDefinitionRegistryPostProcessor.class);
    }

    private void registerConsumerDefinitionRegistryPostProcessor(Set<String> scanBasePackages, BeanDefinitionRegistry registry) {
        registerBeanDefinition(scanBasePackages, registry, ConsumerBeanPostProcessor.class);
    }

    private void registerBeanDefinition(Set<String> scanBasePackages, BeanDefinitionRegistry registry, Class clazz) {
        BeanDefinitionBuilder builder = rootBeanDefinition(clazz);
        builder.addConstructorArgValue(scanBasePackages);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
    }
}
