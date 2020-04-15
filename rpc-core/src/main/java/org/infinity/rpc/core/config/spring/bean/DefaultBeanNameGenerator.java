package org.infinity.rpc.core.config.spring.bean;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

@Slf4j
public class DefaultBeanNameGenerator {

    public static BeanNameGenerator create(BeanDefinitionRegistry registry) {
        log.info("Using the default bean name generator [{}]", AnnotationBeanNameGenerator.class.getName());
        return new AnnotationBeanNameGenerator();
    }
}
