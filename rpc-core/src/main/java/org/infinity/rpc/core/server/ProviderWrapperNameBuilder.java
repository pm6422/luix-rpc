package org.infinity.rpc.core.server;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public final class ProviderWrapperNameBuilder {
    private static final String   PROVIDER_PREFIX = "RpcProvider";
    private static final String   SEPARATOR       = ":";
    private final        Class<?> interfaceClass;

    private final Environment env;

    private ProviderWrapperNameBuilder(Class<?> interfaceClass, Environment env) {
        this.interfaceClass = interfaceClass;
        this.env = env;
    }

    public static ProviderWrapperNameBuilder create(Class<?> interfaceClass, Environment environment) {
        return new ProviderWrapperNameBuilder(interfaceClass, environment);
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
