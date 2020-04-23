package org.infinity.rpc.core.config.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * SpringBoot的SPI扩展在META-INF/spring.factories中配置
 */
@Slf4j
@Deprecated
public class RpcConfigurationApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
//        String scanPackages = env.getProperty(InfinityRpcProperties.SCAN_PACKAGES);
//        Assert.hasText(scanPackages, "Consumer and provider scan base packages must not be empty!");

//        log.debug("RPC scan base package: [{}]", scanPackages);
//        ConsumerBeanPostProcessor springBeanPostProcessor = new ConsumerBeanPostProcessor(scanPackages);
        // Bean post processor will be called before bean factory post processor
//        applicationContext.getBeanFactory().addBeanPostProcessor(springBeanPostProcessor);
//        applicationContext.addBeanFactoryPostProcessor(springBeanPostProcessor);
//        String springBeanPostProcessorBeanName = ClassUtils.getShortNameAsProperty(ConsumerBeanPostProcessor.class);
        // 注意：这里只有注册bean，但由于时机太早，没法注册beanDefinition，所以applicationContext.getBeanDefinitionNames()里获取不到
        // 但applicationContext.getBean(ConsumerAnnotationBean.class)可以获取到bean
        // Register custom bean post processor
//        applicationContext.getBeanFactory().registerSingleton(springBeanPostProcessorBeanName, springBeanPostProcessor);

//        ProviderBeanDefinitionRegistryPostProcessor springBeanDefinitionRegistryPostProcessor = new ProviderBeanDefinitionRegistryPostProcessor(scanPackages);
        // Register custom bean definition post processor
//        String springBeanDefinitionRegistryPostProcessorBeanName = ClassUtils.getShortNameAsProperty(ProviderBeanDefinitionRegistryPostProcessor.class);
//        applicationContext.getBeanFactory().registerSingleton(springBeanDefinitionRegistryPostProcessorBeanName, springBeanDefinitionRegistryPostProcessor);

        log.debug("Initialized consumer annotation bean");
    }
}
