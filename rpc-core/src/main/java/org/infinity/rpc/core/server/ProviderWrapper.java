package org.infinity.rpc.core.server;

public class ProviderWrapper<T> {

    /**
     * The provider interface name
     */
    private String providerInterface;

    /**
     * The provider instance
     */
    private T providerInstance;

    public String getProviderInterface() {
        return providerInterface;
    }

    public void setProviderInterface(String providerInterface) {
        this.providerInterface = providerInterface;
    }

    public T getProviderInstance() {
        return providerInstance;
    }

    public void setProviderInstance(T providerInstance) {
        this.providerInstance = providerInstance;
    }
}
