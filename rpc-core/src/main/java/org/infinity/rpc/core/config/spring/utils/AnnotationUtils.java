package org.infinity.rpc.core.config.spring.utils;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.springframework.core.annotation.AnnotationAttributes.fromMap;
import static org.springframework.core.annotation.AnnotationUtils.getDefaultValue;
import static org.springframework.util.ClassUtils.resolveClassName;
import static org.springframework.util.CollectionUtils.arrayToList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import static org.springframework.util.StringUtils.trimWhitespace;

public abstract class AnnotationUtils {
    /**
     * The class name of AnnotatedElementUtils that is introduced since Spring Framework 4
     */
    public static final String ANNOTATED_ELEMENT_UTILS_CLASS_NAME = "org.springframework.core.annotation.AnnotatedElementUtils";
    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotationAttributes the attributes of specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     */
    public static Map<String, Object> getAttributes(Map<String, Object> annotationAttributes, PropertyResolver propertyResolver, String... ignoreAttributeNames) {

        Set<String> ignoreAttributeNamesSet = new HashSet<>(arrayToList(ignoreAttributeNames));

        Map<String, Object> actualAttributes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> annotationAttribute : annotationAttributes.entrySet()) {

            String attributeName = annotationAttribute.getKey();
            Object attributeValue = annotationAttribute.getValue();

            // ignore attribute name
            if (ignoreAttributeNamesSet.contains(attributeName)) {
                continue;
            }

            if (attributeValue instanceof String) {
                attributeValue = resolvePlaceholders(valueOf(attributeValue), propertyResolver);
            } else if (attributeValue instanceof String[]) {
                String[] values = (String[]) attributeValue;
                for (int i = 0; i < values.length; i++) {
                    values[i] = resolvePlaceholders(values[i], propertyResolver);
                }
                attributeValue = values;
            }
            actualAttributes.put(attributeName, attributeValue);
        }
        return actualAttributes;
    }

    private static String resolvePlaceholders(String attributeValue, PropertyResolver propertyResolver) {
        String resolvedValue = attributeValue;
        if (propertyResolver != null) {
            resolvedValue = propertyResolver.resolvePlaceholders(resolvedValue);
            resolvedValue = trimWhitespace(resolvedValue);
        }
        return resolvedValue;
    }

    /**
     * Get the {@link Annotation} attributes
     *
     * @param annotation           specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @since 1.0.2
     */
    public static Map<String, Object> getAttributes(Annotation annotation, PropertyResolver propertyResolver,
                                                    boolean ignoreDefaultValue, String... ignoreAttributeNames) {

        Map<String, Object> annotationAttributes = org.springframework.core.annotation.AnnotationUtils.getAnnotationAttributes(annotation);

        String[] actualIgnoreAttributeNames = ignoreAttributeNames;

        if (ignoreDefaultValue && !isEmpty(annotationAttributes)) {

            List<String> attributeNamesToIgnore = new LinkedList<String>(asList(ignoreAttributeNames));

            for (Map.Entry<String, Object> annotationAttribute : annotationAttributes.entrySet()) {
                String attributeName = annotationAttribute.getKey();
                Object attributeValue = annotationAttribute.getValue();
                if (nullSafeEquals(attributeValue, getDefaultValue(annotation, attributeName))) {
                    attributeNamesToIgnore.add(attributeName);
                }
            }
            // extends the ignored list
            actualIgnoreAttributeNames = attributeNamesToIgnore.toArray(new String[attributeNamesToIgnore.size()]);
        }

        return getAttributes(annotationAttributes, propertyResolver, actualIgnoreAttributeNames);
    }

    /**
     * Get the {@link AnnotationAttributes}, if the argument <code>tryMergedAnnotation</code> is <code>true</code>,
     * the {@link AnnotationAttributes} will be got from
     * {@link #tryGetMergedAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, String...) merged annotation} first,
     * if failed, and then to get from
     * {@link #getAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, boolean, String...) normal one}
     *
     * @param annotatedElement     {@link AnnotatedElement the annotated element}
     * @param annotationType       the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param tryMergedAnnotation  whether try merged annotation or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return if <code>annotatedElement</code> can't be found in <code>annotatedElement</code>, return <code>null</code>
     * @since 1.0.3
     */
    public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement,
                                                               Class<? extends Annotation> annotationType,
                                                               PropertyResolver propertyResolver,
                                                               boolean ignoreDefaultValue,
                                                               boolean tryMergedAnnotation,
                                                               String... ignoreAttributeNames) {
        AnnotationAttributes attributes = null;

        if (tryMergedAnnotation) {
            attributes = tryGetMergedAnnotationAttributes(annotatedElement, annotationType, propertyResolver, ignoreDefaultValue, ignoreAttributeNames);
        }

        if (attributes == null) {
            attributes = getAnnotationAttributes(annotatedElement, annotationType, propertyResolver, ignoreDefaultValue, ignoreAttributeNames);
        }

        return attributes;
    }

    /**
     * Get the {@link AnnotationAttributes}
     *
     * @param annotatedElement     {@link AnnotatedElement the annotated element}
     * @param annotationType       the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return if <code>annotatedElement</code> can't be found in <code>annotatedElement</code>, return <code>null</code>
     * @since 1.0.3
     */
    public static AnnotationAttributes getAnnotationAttributes(AnnotatedElement annotatedElement,
                                                               Class<? extends Annotation> annotationType,
                                                               PropertyResolver propertyResolver,
                                                               boolean ignoreDefaultValue,
                                                               String... ignoreAttributeNames) {
        Annotation annotation = annotatedElement.getAnnotation(annotationType);
        return annotation == null ? null : getAnnotationAttributes(annotation, propertyResolver, ignoreDefaultValue, ignoreAttributeNames);
    }

    /**
     * Try to get {@link AnnotationAttributes the annotation attributes} after merging and resolving the placeholders
     *
     * @param annotatedElement     {@link AnnotatedElement the annotated element}
     * @param annotationType       the {@link Class tyoe} pf {@link Annotation annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return If the specified annotation type is not found, return <code>null</code>
     * @since 1.0.3
     */
    public static AnnotationAttributes tryGetMergedAnnotationAttributes(AnnotatedElement annotatedElement,
                                                                        Class<? extends Annotation> annotationType,
                                                                        PropertyResolver propertyResolver,
                                                                        boolean ignoreDefaultValue,
                                                                        String... ignoreAttributeNames) {
        Annotation annotation = tryGetMergedAnnotation(annotatedElement, annotationType);
        return annotation == null ? null : getAnnotationAttributes(annotation, propertyResolver, ignoreDefaultValue, ignoreAttributeNames);
    }

    /**
     * Try to get the merged {@link Annotation annotation}
     *
     * @param annotatedElement {@link AnnotatedElement the annotated element}
     * @param annotationType   the {@link Class tyoe} pf {@link Annotation annotation}
     * @return If current version of Spring Framework is below 4.2, return <code>null</code>
     * @since 1.0.3
     */
    public static Annotation tryGetMergedAnnotation(AnnotatedElement annotatedElement,
                                                    Class<? extends Annotation> annotationType) {

        Annotation mergedAnnotation = null;

        ClassLoader classLoader = annotationType.getClassLoader();

        if (ClassUtils.isPresent(ANNOTATED_ELEMENT_UTILS_CLASS_NAME, classLoader)) {
            Class<?> annotatedElementUtilsClass = resolveClassName(ANNOTATED_ELEMENT_UTILS_CLASS_NAME, classLoader);
            // getMergedAnnotation method appears in the Spring Framework 4.2
            Method getMergedAnnotationMethod = findMethod(annotatedElementUtilsClass, "getMergedAnnotation", AnnotatedElement.class, Class.class);
            if (getMergedAnnotationMethod != null) {
                mergedAnnotation = (Annotation) invokeMethod(getMergedAnnotationMethod, null, annotatedElement, annotationType);
            }
        }

        return mergedAnnotation;
    }

    /**
     * Get the {@link AnnotationAttributes}
     *
     * @param annotation           specified {@link Annotation}
     * @param propertyResolver     {@link PropertyResolver} instance, e.g {@link Environment}
     * @param ignoreDefaultValue   whether ignore default value or not
     * @param ignoreAttributeNames the attribute names of annotation should be ignored
     * @return non-null
     * @see #getAttributes(Annotation, PropertyResolver, boolean, String...)
     * @see #getAnnotationAttributes(AnnotatedElement, Class, PropertyResolver, boolean, String...)
     * @since 1.0.3
     */
    public static AnnotationAttributes getAnnotationAttributes(Annotation annotation, PropertyResolver propertyResolver,
                                                               boolean ignoreDefaultValue, String... ignoreAttributeNames) {
        return fromMap(getAttributes(annotation, propertyResolver, ignoreDefaultValue, ignoreAttributeNames));
    }

}
