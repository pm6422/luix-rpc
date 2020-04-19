package org.infinity.rpc.core.server;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

/**
 * PRC provider configuration wrapper
 * Automatically add {@link ProviderWrapper} instance to {@link ProviderWrapperHolder}
 *
 * @param <T>
 */
@Slf4j
public class ProviderWrapper<T> {

    /**
     * The provider interface full qualified name
     */
    private String providerInterface;
    /**
     * The provider instance simple name
     */
    private String providerInstanceName;
    /**
     * The provider instance
     */
    private T      providerInstance;

    /**
     * The method is invoked by Java EE container automatically
     */
    @PostConstruct
    public void init() {
        ProviderWrapperHolder.getInstance().addProvider(providerInterface, this);
    }

    public String getProviderInterface() {
        return providerInterface;
    }

    public void setProviderInterface(String providerInterface) {
        this.providerInterface = providerInterface;
    }

    public String getProviderInstanceName() {
        return providerInstanceName;
    }

    public void setProviderInstanceName(String providerInstanceName) {
        this.providerInstanceName = providerInstanceName;
    }

    public T getProviderInstance() {
        return providerInstance;
    }

    public void setProviderInstance(T providerInstance) {
        this.providerInstance = providerInstance;
    }

    /**
     * Register the RPC provider to registry
     */
    public void register() {
        // todo: register to registry
        log.debug("Registered RPC provider [{}] to registry", providerInterface);
    }
}
