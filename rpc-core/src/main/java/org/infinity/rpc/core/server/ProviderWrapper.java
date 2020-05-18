package org.infinity.rpc.core.server;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.Url;

import javax.annotation.PostConstruct;
import java.util.List;

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
     *
     * @param app          application info
     * @param registryUrls registry urls
     * @param providerUrl  provider url
     */
    public void register(App app, List<Url> registryUrls, Url providerUrl) {
        for (Url registryUrl : registryUrls) {
            // Register provider URL to all the registries
            RegistryFactory registryFactoryImpl = RegistryFactory.getInstance(registryUrl.getProtocol());
            Registry registry = registryFactoryImpl.getRegistry(registryUrl);
            registry.register(providerUrl);
            registry.registerAppProvider(app, providerUrl);
        }
        log.debug("Registered RPC provider [{}] to registry", providerInterface);
    }

    /**
     * Unregister the RPC provider from registry
     */
    public void unregister() {
        log.debug("Unregistered RPC provider [{}] from registry", providerInterface);
    }
}
