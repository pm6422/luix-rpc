package org.infinity.rpc.core.server;

import org.infinity.rpc.core.server.annotation.Provider;

public class ProviderWrapper<T> {

    private Provider provider;

    /**
     * The interface name of the provider
     */
    private String interfaceName;

    /**
     * The reference of the interface implementation
     */
    private T instanceName;

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public T getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(T instanceName) {
        this.instanceName = instanceName;
    }
}
