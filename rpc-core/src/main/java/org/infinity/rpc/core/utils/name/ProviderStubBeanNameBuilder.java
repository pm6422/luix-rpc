package org.infinity.rpc.core.utils.name;

/**
 * RPC provider {@link org.infinity.rpc.core.server.annotation.Provider} stub bean name builder
 * RPC provider interface only can have multiple implementations with different form or version.
 */
public class ProviderStubBeanNameBuilder extends ProviderConsumerStubBeanNameBuilder {

    /**
     * Prevent instantiation of it outside the class
     */
    private ProviderStubBeanNameBuilder(String interfaceClassName) {
        super(interfaceClassName);
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

    public String build() {
        return super.build(PROVIDER_STUB_BEAN_PREFIX);
    }
}
