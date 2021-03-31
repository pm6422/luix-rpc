package org.infinity.rpc.core.utils.name;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Provider {@link org.infinity.rpc.core.server.annotation.Provider @Provider} stub bean Builder
 */
public abstract class ProviderConsumerStubBeanNameBuilder {
    public static final    String PROVIDER_STUB_BEAN_PREFIX = "ProviderStub";
    protected static final String SEPARATOR                 = ":";
    /**
     * Provider interface class(Required)
     */
    protected final        String interfaceClassName;
    /**
     * Group(Optional)
     */
    protected              String form;
    /**
     * Version(Optional)
     */
    protected              String version;

    /**
     * Prevent instantiation of it outside the class
     */
    protected ProviderConsumerStubBeanNameBuilder(String interfaceClassName) {
        Validate.notEmpty(interfaceClassName, "Interface class name must NOT be empty!");
        this.interfaceClassName = interfaceClassName;
    }

    protected String build(String prefix) {
        StringBuilder beanNameBuilder = new StringBuilder(prefix);
        // Required
        append(beanNameBuilder, interfaceClassName);
        // Optional
        append(beanNameBuilder, form);
        append(beanNameBuilder, version);
        return beanNameBuilder.toString();
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
