package org.infinity.rpc.core.config.spring.registrar;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.config.spring.RpcAutoConfiguration;
import org.infinity.rpc.core.config.spring.annotation.EnableRpc;
import org.infinity.rpc.core.config.spring.bean.registry.AnnotatedBeanDefinitionRegistry;
import org.infinity.rpc.core.config.spring.client.ConsumerBeanPostProcessor;
import org.infinity.rpc.core.config.spring.server.ProviderBeanDefinitionRegistryPostProcessor;
import org.infinity.rpc.core.config.spring.startup.RpcLifecycleApplicationListener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
public class RpcProviderConsumerScanRegistrar implements ImportBeanDefinitionRegistrar {

    public RpcProviderConsumerScanRegistrar() {
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Set<String> scanBasePackages = getScanBasePackages(importingClassMetadata);
        registerRpcAutoConfiguration(registry);
        registerRpcLifecycleApplicationListener(registry);
        registerProviderDefinitionRegistryPostProcessor(scanBasePackages, registry);
        registerConsumerDefinitionRegistryPostProcessor(scanBasePackages, registry);
    }

    private Set<String> getScanBasePackages(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableRpc.class.getName()));
        String[] scanBasePackages = attributes.getStringArray("scanBasePackages");
//        Class<?> currentClass = ClassUtils.resolveClassName(metadata.getClassName(), classLoader);
        // Keep sequence
        Set<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(scanBasePackages));
        if (packagesToScan.isEmpty()) {
            String packageName = ClassUtils.getPackageName(metadata.getClassName());
            log.debug("Default scan base package: [{}]", packageName);
            Validate.notEmpty(packageName, "Consumer and provider scan base packages must not be empty!");
            return Collections.singleton(packageName);
        } else {
            log.debug("User defined scan base packages: [{}]", packagesToScan);
        }
        return packagesToScan;
    }

    /**
     * Register RPC auto configuration
     *
     * @param registry
     */
    private void registerRpcAutoConfiguration(BeanDefinitionRegistry registry) {
        AnnotatedBeanDefinitionRegistry.registerBeans(registry, RpcAutoConfiguration.class);
    }

    /**
     * Register RPC provider and consumer start and stop listener
     *
     * @param registry
     */
    private void registerRpcLifecycleApplicationListener(BeanDefinitionRegistry registry) {
        AnnotatedBeanDefinitionRegistry.registerBeans(registry, RpcLifecycleApplicationListener.class);
    }

    /**
     * Register beans with @Provider annotation
     *
     * @param scanBasePackages
     * @param registry
     */
    private void registerProviderDefinitionRegistryPostProcessor(Set<String> scanBasePackages, BeanDefinitionRegistry registry) {
        registerBeanDefinition(scanBasePackages, registry, ProviderBeanDefinitionRegistryPostProcessor.class);
    }

    /**
     * Register beans with @Consumer annotation
     *
     * @param scanBasePackages
     * @param registry
     */
    private void registerConsumerDefinitionRegistryPostProcessor(Set<String> scanBasePackages, BeanDefinitionRegistry registry) {
        registerBeanDefinition(scanBasePackages, registry, ConsumerBeanPostProcessor.class);
    }

    private void registerBeanDefinition(Set<String> scanBasePackages, BeanDefinitionRegistry registry, Class clazz) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(clazz);
        builder.addConstructorArgValue(scanBasePackages);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
    }
}
