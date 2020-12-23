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

import javax.annotation.Nonnull;
import java.util.*;

@Slf4j
public class RpcProviderConsumerScanRegistrar implements ImportBeanDefinitionRegistrar {

    public RpcProviderConsumerScanRegistrar() {
    }

    /**
     * Register service providers and consumers bean definitions
     *
     * @param importingClassMetadata annotation metadata of the importing class
     * @param registry               current bean definition registry
     */
    @Override
    public void registerBeanDefinitions(@Nonnull AnnotationMetadata importingClassMetadata,
                                        @Nonnull BeanDefinitionRegistry registry) {
        Set<String> scanBasePackages = getScanBasePackages(importingClassMetadata);
        registerRpcAutoConfiguration(registry);
        registerRpcLifecycleApplicationListener(registry);
        registerProviderBeanDefinitionRegistryPostProcessor(scanBasePackages, registry);
        registerConsumerBeanPostProcessor(scanBasePackages, registry);
//        registerConsumerAnnotationBeanPostProcessor(registry);
    }

    /**
     * Get the packages to be scanned for service providers and consumers
     *
     * @param metadata annotation metadata
     * @return packages to be scanned
     */
    private Set<String> getScanBasePackages(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableRpc.class.getName()));
        String[] scanBasePackages = Objects.requireNonNull(attributes).getStringArray("scanBasePackages");
        // Keep order
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
     * Register bean of RPC auto configuration
     *
     * @param registry current bean definition registry
     */
    private void registerRpcAutoConfiguration(BeanDefinitionRegistry registry) {
        AnnotatedBeanDefinitionRegistry.registerBeans(registry, RpcAutoConfiguration.class);
    }

    /**
     * Register bean of RPC lifecycle listener
     *
     * @param registry current bean definition registry
     */
    private void registerRpcLifecycleApplicationListener(BeanDefinitionRegistry registry) {
        AnnotatedBeanDefinitionRegistry.registerBeans(registry, RpcLifecycleApplicationListener.class);
    }

    /**
     * Register bean definition of service providers with @Provider annotation
     *
     * @param scanBasePackages packages to be scanned
     * @param registry         current bean definition registry
     */
    private void registerProviderBeanDefinitionRegistryPostProcessor(Set<String> scanBasePackages, BeanDefinitionRegistry registry) {
        registerBeanDefinition(scanBasePackages, registry, ProviderBeanDefinitionRegistryPostProcessor.class);
    }

    /**
     * Register bean definition of service consumers with @Consumer annotation
     *
     * @param scanBasePackages packages to be scanned
     * @param registry         current bean definition registry
     */
    private void registerConsumerBeanPostProcessor(Set<String> scanBasePackages, BeanDefinitionRegistry registry) {
        registerBeanDefinition(scanBasePackages, registry, ConsumerBeanPostProcessor.class);
    }

    /**
     * @param scanBasePackages packages to be scanned
     * @param registry         current bean definition registry
     * @param clazz            class to be registered
     */
    private void registerBeanDefinition(Set<String> scanBasePackages, BeanDefinitionRegistry registry, Class<?> clazz) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(clazz);
        builder.addConstructorArgValue(scanBasePackages);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
    }

    //    /**
//     * Registers {@link ConsumerAnnotationBeanPostProcessor} into {@link BeanFactory}
//     *
//     * @param registry {@link BeanDefinitionRegistry}
//     */
//    private void registerConsumerAnnotationBeanPostProcessor(BeanDefinitionRegistry registry) {
//        registerInfrastructureBean(registry, ConsumerAnnotationBeanPostProcessor.BEAN_NAME, ConsumerAnnotationBeanPostProcessor.class);
//    }


}
