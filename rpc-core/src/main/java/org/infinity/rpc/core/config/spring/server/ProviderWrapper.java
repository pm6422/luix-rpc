package org.infinity.rpc.core.config.spring.server;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.server.ProviderWrapperHolder;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;

/**
 * PRC provider configuration wrapper
 *
 * @param <T>
 */
@Slf4j
@Data
public class ProviderWrapper<T> implements DisposableBean {
    /**
     * The provider interface fully-qualified name
     */
    private String   interfaceName;
    /**
     * The interface class of the provider
     */
    private Class<?> interfaceClass;
    /**
     * The provider instance simple name, also known as bean name
     */
    private String   instanceName;
    /**
     * The provider instance
     */
    private T        instance;

    /**
     * The method is invoked by Java EE container automatically after registered bean definition
     * Automatically add {@link ProviderWrapper} instance to {@link ProviderWrapperHolder}
     */
    @PostConstruct
    public void init() {
        ProviderWrapperHolder.getInstance().addWrapper(interfaceName, this);
    }

    @Override
    public void destroy() throws Exception {
        // Leave blank intentionally for now
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
            registry.registerApplicationProvider(app, providerUrl);
        }
        log.debug("Registered RPC provider [{}] to registry", interfaceName);
    }

    /**
     * Unregister the RPC provider from registry
     */
    public void unregister(List<Url> registryUrls) {
        // TODO: the method is never be invoked
        for (Url registryUrl : registryUrls) {
            RegistryFactory registryFactoryImpl = RegistryFactory.getInstance(registryUrl.getProtocol());
            Registry registry = registryFactoryImpl.getRegistry(registryUrl);
            Set<Url> registeredProviderUrls = registry.getRegisteredProviderUrls();
            if (CollectionUtils.isEmpty(registeredProviderUrls)) {
                return;
            }
            registeredProviderUrls.forEach(registeredProviderUrl -> {
                registry.unregister(registeredProviderUrl);
            });
        }
        log.debug("Unregistered RPC provider [{}] from registry", interfaceName);
    }
}
