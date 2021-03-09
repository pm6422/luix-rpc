package org.infinity.rpc.core.protocol;

import org.infinity.rpc.core.client.request.Importable;
import org.infinity.rpc.core.server.exporter.Exportable;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

@Spi(scope = SpiScope.SINGLETON)
public interface Protocol<T> {

    /**
     * Create provider exporter
     *
     * @param providerStub provider stub
     * @return exporter
     */
    Exportable<T> createExporter(ProviderStub<T> providerStub);

    /**
     * Create provider importer
     *
     * @param interfaceName provider interface name
     * @param providerUrl   provider url
     * @return provider importer
     */
    Importable createImporter(String interfaceName, Url providerUrl);

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
    @SuppressWarnings("rawtypes")
    static Protocol getInstance(String name) {
        return ServiceLoader.forClass(Protocol.class).load(name);
    }
}
