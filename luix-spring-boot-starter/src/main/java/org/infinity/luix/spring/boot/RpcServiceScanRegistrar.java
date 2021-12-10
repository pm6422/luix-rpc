package org.infinity.luix.spring.boot;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.spring.boot.bean.ConsumerBeanPostProcessor;
import org.infinity.luix.spring.boot.bean.ProviderBeanDefinitionRegistryPostProcessor;
import org.infinity.luix.spring.boot.bean.registry.AnnotatedBeanDefinitionRegistry;
import org.infinity.luix.spring.boot.startup.RpcLifecycleApplicationListener;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.*;

@Slf4j
public class RpcServiceScanRegistrar implements ImportBeanDefinitionRegistrar {

    public RpcServiceScanRegistrar() {
    }

    /**
     * Register service providers and consumers bean definitions
     *
     * @param importingClassMetadata annotation metadata of the importing class
     * @param registry               current bean definition registry
     */
    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                        @NonNull BeanDefinitionRegistry registry) {
        Set<String> scanBasePackages = getScanBasePackages(importingClassMetadata);
        registerRpcAutoConfiguration(registry);
        registerRpcLifecycleApplicationListener(registry);
        registerProviderBeanDefinitionRegistryPostProcessor(registry, scanBasePackages);
        registerConsumerBeanPostProcessor(registry, scanBasePackages);
//        registerConsumerAnnotationBeanPostProcessor(registry);
    }

    /**
     * Get the packages to be scanned for service providers and consumers
     *
     * @param metadata annotation metadata
     * @return packages to be scanned
     */
    private Set<String> getScanBasePackages(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableLuixRpc.class.getName()));
        String[] scanBasePackages = Objects.requireNonNull(attributes).getStringArray("scanBasePackages");
        // Keep order
        Set<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(scanBasePackages));
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
     * @param registry         current bean definition registry
     * @param scanBasePackages packages to be scanned
     */
    private void registerProviderBeanDefinitionRegistryPostProcessor(BeanDefinitionRegistry registry,
                                                                     Set<String> scanBasePackages) {
        registerBeanDefinition(registry, ProviderBeanDefinitionRegistryPostProcessor.class, scanBasePackages);
    }

    /**
     * Register bean definition of service consumers with @Consumer annotation
     *
     * @param registry         current bean definition registry
     * @param scanBasePackages packages to be scanned
     */
    private void registerConsumerBeanPostProcessor(BeanDefinitionRegistry registry, Set<String> scanBasePackages) {
        registerBeanDefinition(registry, ConsumerBeanPostProcessor.class, scanBasePackages);
    }

    /**
     * @param scanBasePackages packages to be scanned
     * @param registry         current bean definition registry
     * @param beanType         class to be registered
     */
    private void registerBeanDefinition(BeanDefinitionRegistry registry, Class<?> beanType,
                                        Set<String> scanBasePackages) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(beanType);
        if (scanBasePackages != null) {
            builder.addConstructorArgValue(scanBasePackages);
        }
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
