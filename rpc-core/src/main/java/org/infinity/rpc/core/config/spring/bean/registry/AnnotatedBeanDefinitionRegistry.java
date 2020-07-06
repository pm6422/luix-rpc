package org.infinity.rpc.core.config.spring.bean.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Utilities class used to register bean
 */
@Slf4j
public abstract class AnnotatedBeanDefinitionRegistry {

    /**
     * Register beans to {@link BeanDefinitionRegistry registry}
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @param classes  classes to register
     */
    public static void registerBeans(BeanDefinitionRegistry registry, Class<?>... classes) {
        Class<?>[] filteredClasses = filterNotYetRegistered(registry, classes);
        // AnnotatedBeanDefinitionReader is an alternative to ClassPathBeanDefinitionScanner, used to register custom bean
        AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);
        log.debug("{} registered classes: {}", registry.getClass().getSimpleName(), Arrays.asList(classes));
        reader.register(filteredClasses);
    }

    private static Class<?>[] filterNotYetRegistered(BeanDefinitionRegistry registry, Class<?>[] classes) {
        return Arrays.asList(classes).stream().filter(clazz -> !registered(registry, clazz)).toArray(Class<?>[]::new);
    }

    /**
     * Register Infrastructure Bean
     *
     * @param beanDefinitionRegistry {@link BeanDefinitionRegistry}
     * @param beanType               the type of bean
     * @param beanName               the name of bean
     * @return if registered at first time, return <code>true</code>, or <code>false</code>
     */
    public static boolean registerInfrastructureBean(BeanDefinitionRegistry beanDefinitionRegistry, String beanName, Class<?> beanType) {
        if (beanDefinitionRegistry.containsBeanDefinition(beanName)) {
            return false;
        }
        RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType);
        // Set a INFRASTRUCTURE role
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);

        log.info("Registered the infrastructure bean definition [{}] with name [{}]", beanDefinition, beanName);
        return true;
    }

    /**
     * Determine the class whether has been registered
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @param clazz    the {@link Annotation annotated} {@link Class class}
     * @return if registered, return <code>true</code>, or <code>false</code>
     */
    public static boolean registered(BeanDefinitionRegistry registry, Class<?> clazz) {
        boolean registered = false;
        for (String beanDefinitionName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
            if (beanDefinition instanceof AnnotatedBeanDefinition) {
                AnnotationMetadata annotationMetadata = ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
                String className = annotationMetadata.getClassName();
                Class<?> targetClass = ClassUtils.resolveClassName(className, clazz.getClassLoader());
                registered = ObjectUtils.nullSafeEquals(targetClass, clazz);
                if (registered) {
                    log.info("The class [{}] with the bean name [{}] has been registered on registry [{}]!", className, beanDefinitionName, registry);
                    break;
                }
            }
        }
        return registered;
    }
}
