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

    <T> ProviderCaller<T> createProviderCaller(Class<T> interfaceClass, Url providerUrl);

    /**
     * 暴露服务
     *
     * @param <T>
     * @param providerWrapper
     * @return
     */
    <T> Exportable<T> export(ProviderWrapper<T> providerWrapper);

    void destroy();

    /**
     * Get the protocol instance based on protocol name
     *
     * @param protocolName protocol name
     * @return protocol instance
     */
    static Protocol getInstance(String protocolName) {
        // Get the proper protocol by protocol name
        Protocol protocol = ServiceLoader.forClass(Protocol.class).load(protocolName);
        return protocol;
    }
}
