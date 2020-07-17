package org.infinity.rpc.core.config.spring.server;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

/**
 * Consumer {@link org.infinity.rpc.core.server.annotation.Provider @Provider} wrapper bean Builder
 */
public final class ProviderWrapperBeanNameBuilder {
    public static final  String      PROVIDER_WRAPPER_BEAN_PREFIX = "ProviderWrapperBean";
    private static final String      SEPARATOR                    = ":";
    // Required
    private final        Class<?>    interfaceClass;
    private final        Environment env;

    /**
     * Prohibit instantiate an instance outside the class
     */
    private ProviderWrapperBeanNameBuilder(Class<?> interfaceClass, Environment env) {
        this.interfaceClass = interfaceClass;
        this.env = env;
    }

    public static ProviderWrapperBeanNameBuilder builder(Class<?> interfaceClass, Environment environment) {
        return new ProviderWrapperBeanNameBuilder(interfaceClass, environment);
    }

    private static void append(StringBuilder builder, String value) {
        if (StringUtils.isNotEmpty(value)) {
            builder.append(SEPARATOR).append(value);
        }
    }

    public String build() {
        StringBuilder nameBuilder = new StringBuilder(PROVIDER_WRAPPER_BEAN_PREFIX);
        append(nameBuilder, interfaceClass.getName());
        String name = nameBuilder.toString();
        return env.resolvePlaceholders(name);
    }
}
