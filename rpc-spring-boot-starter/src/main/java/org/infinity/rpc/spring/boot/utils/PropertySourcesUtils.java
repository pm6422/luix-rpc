package org.infinity.rpc.spring.boot.utils;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.infinity.rpc.core.config.Configurable;
import org.infinity.rpc.core.exception.impl.RpcConfigurationException;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static java.util.Collections.unmodifiableMap;

public abstract class PropertySourcesUtils {

    /**
     * Get prefixed {@link Properties}
     *
     * @param propertySources  {@link PropertySources}
     * @param propertyResolver {@link PropertyResolver} to resolve the placeholder if present
     * @param prefix           the prefix of property name
     * @return Map
     * @see Properties
     */
    public static Map<String, Object> getSubProperties(PropertySources propertySources, PropertyResolver propertyResolver, String prefix) {
        Map<String, Object> subProperties = new LinkedHashMap<>();
        String normalizedPrefix = normalizePrefix(prefix);
        for (PropertySource<?> source : propertySources) {
            if (!(source instanceof EnumerablePropertySource)) {
                continue;
            }
            for (String name : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                if (!name.startsWith(normalizedPrefix) || subProperties.containsKey(name)) {
                    continue;
                }
                String subName = name.substring(normalizedPrefix.length());
                if (!subProperties.containsKey(subName)) {
                    Object value = source.getProperty(name);
                    if (value instanceof String) {
                        // Resolve placeholder
                        value = propertyResolver.resolvePlaceholders((String) value);
                    }
                    subProperties.put(subName, value);
                }
            }
        }
        return unmodifiableMap(subProperties);
    }

    /**
     * Normalize the prefix
     *
     * @param prefix the prefix
     * @return the prefix
     */
    public static String normalizePrefix(String prefix) {
        return prefix.endsWith(".") ? prefix : prefix + ".";
    }

    /**
     * Read prefixed {@link Properties}
     *
     * @param propertySources  {@link PropertySources}
     * @param propertyResolver {@link PropertyResolver} to resolve the placeholder if present
     * @param prefix           the prefix of property name
     * @param configClz        Class
     * @param config           Configurable
     */
    public static void readProperties(PropertySources propertySources,
                                      PropertyResolver propertyResolver,
                                      String prefix,
                                      Class<?> configClz,
                                      Configurable config) {
        Map<String, Object> properties = getSubProperties(propertySources, propertyResolver, prefix);
        if (MapUtils.isEmpty(properties)) {
            return;
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            try {
                FieldUtils.writeField(configClz.getDeclaredField(entry.getKey()), config, entry.getValue(), true);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RpcConfigurationException("Failed to set configuration property: " + entry.getKey());
            }
        }
    }
}
