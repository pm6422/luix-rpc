package org.infinity.springboot.infinityrpc;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.SpringBeanDefinitionRegistryPostProcessor;
import org.infinity.rpc.client.SpringBeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * SpringBoot的SPI扩展在META-INF/spring.factories中配置
 */
@Slf4j
public class RpcConfigurationApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String consumerScanPackages = env.getProperty(InfinityRpcProperties.CONSUMER_SCAN_PACKAGES);
        Assert.hasText(consumerScanPackages, "Consumer scan base packages must not be empty!");

        log.debug("RPC consumer scan base package: [{}]", consumerScanPackages);
        SpringBeanPostProcessor springBeanPostProcessor = new SpringBeanPostProcessor(applicationContext, consumerScanPackages);
        // Bean post processor will be called before bean factory post processor
        applicationContext.getBeanFactory().addBeanPostProcessor(springBeanPostProcessor);
        applicationContext.addBeanFactoryPostProcessor(springBeanPostProcessor);
        String springBeanPostProcessorBeanName = ClassUtils.getShortNameAsProperty(SpringBeanPostProcessor.class);
        // 注意：这里只有注册bean，但由于时机太早，没法注册beanDefinition，所以applicationContext.getBeanDefinitionNames()里获取不到
        // 但applicationContext.getBean(ConsumerAnnotationBean.class)可以获取到bean
        // Register custom bean post processor
        applicationContext.getBeanFactory().registerSingleton(springBeanPostProcessorBeanName, springBeanPostProcessor);

        SpringBeanDefinitionRegistryPostProcessor springBeanDefinitionRegistryPostProcessor = new SpringBeanDefinitionRegistryPostProcessor();
        // Register custom bean definition post processor
        String springBeanDefinitionRegistryPostProcessorBeanName = ClassUtils.getShortNameAsProperty(SpringBeanDefinitionRegistryPostProcessor.class);
        applicationContext.getBeanFactory().registerSingleton(springBeanDefinitionRegistryPostProcessorBeanName, springBeanDefinitionRegistryPostProcessor);

        log.debug("Initialized consumer annotation bean");
    }
}
