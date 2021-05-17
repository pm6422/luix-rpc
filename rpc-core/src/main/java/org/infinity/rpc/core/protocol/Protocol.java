package org.infinity.rpc.core.protocol;

import org.infinity.rpc.core.client.request.Invokable;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.serviceloader.ServiceLoader;
import org.infinity.rpc.utilities.serviceloader.annotation.Spi;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiScope;

@Spi(scope = SpiScope.SINGLETON)
public interface Protocol {

    /**
     * Create provider exporter
     *
     * @param providerStub provider stub
     * @return exporter
     */
    <T> Exportable<T> export(ProviderStub<T> providerStub);

    /**
     * Create provider invoker
     *
     * @param interfaceName provider interface name
     * @param providerUrl   provider url
     * @return provider invoker
     */
    Invokable refer(String interfaceName, Url providerUrl);

    /**
     * Destroy
     */
    void destroy();

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static Protocol getInstance(String name) {
        return ServiceLoader.forClass(Protocol.class).load(name);
    }
}
