package org.infinity.rpc.spring.boot.utils;

import org.infinity.rpc.core.constant.BooleanEnum;
import org.infinity.rpc.core.exception.impl.RpcConfigException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Refer to {@link org.springframework.beans.annotation.AnnotationBeanUtils}
 */
public abstract class AnnotationBeanDefinitionUtils {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    public static void copyPropertiesToBeanDefinitionBuilder(Annotation ann, BeanDefinitionBuilder builder, String... excludedProperties) {
        Set<String> excluded = (excludedProperties.length == 0 ? Collections.emptySet() : new HashSet<>(Arrays.asList(excludedProperties)));
        Method[] annotationProperties = ann.annotationType().getDeclaredMethods();
        for (Method annotationProperty : annotationProperties) {
            String propertyName = annotationProperty.getName();
            if (!excluded.contains(propertyName)) {
                Object value = ReflectionUtils.invokeMethod(annotationProperty, ann);
                if (value instanceof BooleanEnum) {
                    addPropertyValue(builder, propertyName, ((BooleanEnum) value).getValue());
                } else {
                    addPropertyValue(builder, propertyName, value);
                }
            }
        }
    }

    public static void addPropertyValue(BeanDefinitionBuilder builder, String propertyName, Object propertyValue) {
        validatePropertyValue(builder.getBeanDefinition().getBeanClass(), propertyName, propertyValue);
        builder.addPropertyValue(propertyName, propertyValue);
    }

    private static void validatePropertyValue(Class<?> beanType, String propertyName, Object propertyValue) {
        try {
            List<String> messages = doValidate(beanType, propertyName, propertyValue);
            Assert.isTrue(CollectionUtils.isEmpty(messages), String.join(",", messages));
        } catch (Exception e) {
            // Re-throw the exception
            throw new RpcConfigException(e.getMessage());
        }
    }

    private static <T> List<String> doValidate(Class<T> beanType, String propertyName, Object propertyValue) {
        Set<ConstraintViolation<T>> constraintViolations = VALIDATOR_FACTORY.getValidator()
                .validateValue(beanType, propertyName, propertyValue);
        return constraintViolations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
    }

    /**
     * Obtain the instance by instance name then assign it to the property
     *
     * @param builder      bean definition builder
     * @param propertyName property name
     * @param instanceName provider instance name
     */
    public static void addPropertyReference(BeanDefinitionBuilder builder, String propertyName, String instanceName, Environment env) {
        builder.addPropertyReference(propertyName, env.resolvePlaceholders(instanceName));
    }
}
