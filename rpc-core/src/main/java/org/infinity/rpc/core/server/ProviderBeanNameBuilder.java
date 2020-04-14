package org.infinity.rpc.core.server;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public final class ProviderBeanNameBuilder {
    private static final String   PROVIDER_PREFIX = "RpcProvider";
    private static final String   SEPARATOR       = ":";
    private final        Class<?> interfaceClass;

    private final Environment env;

    private ProviderBeanNameBuilder(Class<?> interfaceClass, Environment env) {
        this.interfaceClass = interfaceClass;
        this.env = env;
    }

    public static ProviderBeanNameBuilder create(Class<?> interfaceClass, Environment environment) {
        return new ProviderBeanNameBuilder(interfaceClass, environment);
    }

    public String build() {
        StringBuilder nameBuilder = new StringBuilder(PROVIDER_PREFIX);
        append(nameBuilder, interfaceClass.getName());
        String name = nameBuilder.toString();
        return env.resolvePlaceholders(name);
    }

    private static void append(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(SEPARATOR).append(value);
        }
    }
}
