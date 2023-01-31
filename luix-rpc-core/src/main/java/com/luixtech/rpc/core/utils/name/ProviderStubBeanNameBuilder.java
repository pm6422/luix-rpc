package com.luixtech.rpc.core.utils.name;

import com.luixtech.rpc.core.server.annotation.RpcProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * RPC provider {@link RpcProvider} stub bean name builder
 * RPC provider interface only can have multiple implementations with different form or version.
 */
public class ProviderStubBeanNameBuilder {
    public static final    String  PREFIX    = "ProviderStub";
    protected static final String  SEPARATOR = ":";
    /**
     * Provider interface class(Required)
     */
    protected final        String  interfaceClassName;
    /**
     * Group(Optional)
     */
    protected              String  form;
    /**
     * Version(Optional)
     */
    protected              String  version;
    /**
     * Use prefix indicator
     */
    protected              boolean usePrefix = true;


    /**
     * Prevent instantiation of it outside the class
     */
    private ProviderStubBeanNameBuilder(String interfaceClassName) {
        Validate.notEmpty(interfaceClassName, "Interface class name must NOT be empty!");
        this.interfaceClassName = interfaceClassName;
    }

    public static ProviderStubBeanNameBuilder builder(String interfaceClassName) {
        return new ProviderStubBeanNameBuilder(interfaceClassName);
    }

    public ProviderStubBeanNameBuilder form(String form) {
        this.form = form;
        return this;
    }

    public ProviderStubBeanNameBuilder version(String version) {
        this.version = version;
        return this;
    }

    public ProviderStubBeanNameBuilder disablePrefix() {
        this.usePrefix = false;
        return this;
    }

    public String build() {
        StringBuilder beanNameBuilder = new StringBuilder(usePrefix ? PREFIX : StringUtils.EMPTY);
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
