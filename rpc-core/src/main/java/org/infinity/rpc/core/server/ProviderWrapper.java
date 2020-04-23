package org.infinity.rpc.core.server;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;

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
     * The provider interface fully-qualified name
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
     * The method is invoked by Java EE container automatically after registered bean definition
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
    public void register(Url url) {
        RegistryFactory registryFactory = getRegistryFactory(url);
        Registry registry = registryFactory.getRegistry(url);
        registry.register(url);
        log.debug("Registered RPC provider [{}] to registry", providerInterface);
    }

    private RegistryFactory getRegistryFactory(Url url) {
        // Get the property registry factory by protocol value
        RegistryFactory registryFactory = ServiceInstanceLoader.getServiceLoader(RegistryFactory.class).getServiceImpl(url.getProtocol());
        if(registryFactory ==null) {
            throw new IllegalStateException("Failed to find the proper registry factory, please check the dependency or a correct protocol value!");
        }
        return registryFactory;
    }

    /**
     * Unregister the RPC provider from registry
     */
    public void unregister() {
        // todo: unregister to registry
        log.debug("Unregistered RPC provider [{}] from registry", providerInterface);
    }
}
