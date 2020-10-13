package org.infinity.rpc.core.protocol;

import org.infinity.rpc.core.exchange.request.ProviderCaller;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

@Spi(scope = SpiScope.SINGLETON)
public interface Protocol {

    <T> ProviderCaller<T> createProviderCaller(Class<T> interfaceClass, Url providerUrl);

    //    <T> Exporter<T> createExporter(Provider<T> provider, URL url);

    void destroy();

    /**
     * Get the protocol instance based on protocol name
     *
     * @param protocolName protocol name
     * @return protocol instance
     */
    static Protocol getInstance(String protocolName) {
        // Get the proper protocol by protocol name
        Protocol protocol = ServiceInstanceLoader.getServiceLoader(Protocol.class).load(protocolName);
        return protocol;
    }
}
