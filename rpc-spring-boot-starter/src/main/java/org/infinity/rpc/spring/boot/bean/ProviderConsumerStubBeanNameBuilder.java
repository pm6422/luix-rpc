package org.infinity.rpc.spring.boot.bean;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * Provider {@link org.infinity.rpc.core.server.annotation.Provider @Provider} stub bean Builder
 * Consumer {@link org.infinity.rpc.core.client.annotation.Consumer @Consumer} stub bean Builder
 */
public abstract class ProviderConsumerStubBeanNameBuilder {
    public static final    String      PROVIDER_STUB_BEAN_PREFIX = "ProviderStub";
    public static final    String      CONSUMER_STUB_BEAN_PREFIX = "ConsumerStub";
    protected static final String      SEPARATOR                 = ":";
    /**
     * Provider interface class(Required)
     */
    protected final        String      interfaceClassName;
    /**
     * Environment(Required)
     */
    protected final        Environment env;
    /**
     * Group(Optional)
     */
    protected              String      group;
    /**
     * Version(Optional)
     */
    protected              String      version;

    /**
     * Prevent instantiation of it outside the class
     */
    protected ProviderConsumerStubBeanNameBuilder(String interfaceClassName, Environment env) {
        Assert.hasText(interfaceClassName, "Interface class name must NOT be empty!");
        Assert.notNull(env, "Environment must NOT be null!");
        this.interfaceClassName = interfaceClassName;
        this.env = env;
    }

    protected String build(String prefix) {
        StringBuilder beanNameBuilder = new StringBuilder(prefix);
        // Required
        append(beanNameBuilder, interfaceClassName);
        // Optional
        append(beanNameBuilder, group);
        append(beanNameBuilder, version);
        String rawBeanName = beanNameBuilder.toString();
        // Resolve placeholders
        return env.resolvePlaceholders(rawBeanName);
    }

    protected static void append(StringBuilder builder, String value) {
        if (StringUtils.isNotEmpty(value)) {
            if (builder.length() > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(value);
        }
    }
}
