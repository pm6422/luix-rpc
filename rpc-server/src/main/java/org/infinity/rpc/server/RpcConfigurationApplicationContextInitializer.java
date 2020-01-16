package org.infinity.rpc.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

/**
 * SpringBoot的SPI扩展在META-INF/spring.factories中配置
 */
@Slf4j
public class RpcConfigurationApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        ProviderAnnotationBean providerAnnotationBean = new ProviderAnnotationBean(applicationContext);
        // Bean post processor will be called before bean factory post processor
        applicationContext.getBeanFactory().addBeanPostProcessor(providerAnnotationBean);
        applicationContext.addBeanFactoryPostProcessor(providerAnnotationBean);
        String beanName = ClassUtils.getShortNameAsProperty(ProviderAnnotationBean.class);
        // Register bean
        // 注意：这里只有注册bean，但由于时机太早，没法注册beanDefinition，所以applicationContext.getBeanDefinitionNames()里获取不到
        // 但applicationContext.getBean(ConsumerAnnotationBean.class)可以获取到bean
        applicationContext.getBeanFactory().registerSingleton(beanName, providerAnnotationBean);
        log.debug("Initialized provider annotation bean");
    }
}
