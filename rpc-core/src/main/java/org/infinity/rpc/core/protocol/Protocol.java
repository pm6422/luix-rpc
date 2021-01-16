package org.infinity.rpc.core.protocol;

import org.infinity.rpc.core.config.spring.server.exporter.Exportable;
import org.infinity.rpc.core.config.spring.server.providerwrapper.ProviderWrapper;
import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

@Spi(scope = SpiScope.SINGLETON)
public interface Protocol {

    /**
     * Create provider caller
     *
     * @param interfaceClass provider interface
     * @param providerUrl    provider url
     * @param <T> provider instance
     * @return provider caller
     */
    <T> ProviderCaller<T> createProviderCaller(Class<T> interfaceClass, Url providerUrl);

    /**
     * 暴露服务
     *
     * @param <T>             provider interface
     * @param providerWrapper provider wrapper
     * @return exporter
     */
    <T> Exportable<T> export(ProviderWrapper<T> providerWrapper);

    /**
     * Destroy
     */
    void destroy();

    /**
     * Get protocol instance associated with the specified name
     *
     * @param name specified protocol name
     * @return protocol instance
     */
    static Protocol getInstance(String name) {
        return ServiceLoader.forClass(Protocol.class).load(name);
    }
}
