package org.infinity.rpc.core.config.spring.server.stub;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

/**
 * Consumer {@link org.infinity.rpc.core.server.annotation.Provider @Provider} stub bean Builder
 */
public final class ProviderStubBeanNameBuilder {
    public static final  String      PROVIDER_STUB_BEAN_PREFIX = "ProviderStub";
    private static final String      SEPARATOR                 = ":";
    /**
     * Provider interface class(Required)
     */
    private final        Class<?>    interfaceClass;
    private final        Environment env;

    /**
     * Prevent instantiation of it outside the class
     */
    private ProviderStubBeanNameBuilder(Class<?> interfaceClass, Environment env) {
        this.interfaceClass = interfaceClass;
        this.env = env;
    }

    public static ProviderStubBeanNameBuilder builder(Class<?> interfaceClass, Environment environment) {
        return new ProviderStubBeanNameBuilder(interfaceClass, environment);
    }

    private static void append(StringBuilder builder, String value) {
        if (StringUtils.isNotEmpty(value)) {
            builder.append(SEPARATOR).append(value);
        }
    }

    public String build() {
        StringBuilder nameBuilder = new StringBuilder(PROVIDER_STUB_BEAN_PREFIX);
        append(nameBuilder, interfaceClass.getName());
        String name = nameBuilder.toString();
        return env.resolvePlaceholders(name);
    }
}
