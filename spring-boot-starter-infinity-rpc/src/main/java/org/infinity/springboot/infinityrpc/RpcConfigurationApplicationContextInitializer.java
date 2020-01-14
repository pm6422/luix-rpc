package org.infinity.springboot.infinityrpc;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.client.ConsumerAnnotationBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * SpringBoot的SPI扩展在META-INF/spring.factories中配置
 */
@Slf4j
public class RpcConfigurationApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String consumerScanBasePackages = env.getProperty(RpcClientProperties.CONSUMER_SCAN_BASE_PACKAGES);
        log.debug("RPC client scan package: [{}]", consumerScanBasePackages);
        if (!StringUtils.isEmpty(consumerScanBasePackages)) {
            ConsumerAnnotationBean consumerAnnotationBean = new ConsumerAnnotationBean(applicationContext, consumerScanBasePackages);
            // Bean post processor will be called before bean factory post processor
            applicationContext.getBeanFactory().addBeanPostProcessor(consumerAnnotationBean);
            applicationContext.addBeanFactoryPostProcessor(consumerAnnotationBean);
            String beanName = ClassUtils.getShortNameAsProperty(ConsumerAnnotationBean.class);
            // Register bean
            applicationContext.getBeanFactory().registerSingleton(beanName, consumerAnnotationBean);
            log.debug("Initialized consumer annotation bean");
        }
    }
}
