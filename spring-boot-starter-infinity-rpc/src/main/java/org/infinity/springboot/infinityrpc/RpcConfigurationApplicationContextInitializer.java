package org.infinity.springboot.infinityrpc;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.ConsumerAnnotationBean;
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
        String consumerScanPackages = env.getProperty(RpcClientProperties.CONSUMER_SCAN_PACKAGES);
        Assert.hasText(consumerScanPackages, "Consumer scan base packages must not be empty!");

        log.debug("RPC client scan package: [{}]", consumerScanPackages);
        ConsumerAnnotationBean consumerAnnotationBean = new ConsumerAnnotationBean(applicationContext, consumerScanPackages);
        // Bean post processor will be called before bean factory post processor
        applicationContext.getBeanFactory().addBeanPostProcessor(consumerAnnotationBean);
        applicationContext.addBeanFactoryPostProcessor(consumerAnnotationBean);
        String beanName = ClassUtils.getShortNameAsProperty(ConsumerAnnotationBean.class);
        // Register bean
        applicationContext.getBeanFactory().registerSingleton(beanName, consumerAnnotationBean);
        log.debug("Initialized consumer annotation bean");
    }
}
