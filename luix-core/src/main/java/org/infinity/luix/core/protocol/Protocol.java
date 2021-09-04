package org.infinity.luix.core.protocol;

import org.infinity.luix.core.server.exporter.Exportable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.client.sender.Sendable;
import org.infinity.luix.utilities.serviceloader.ServiceLoader;
import org.infinity.luix.utilities.serviceloader.annotation.Spi;
import org.infinity.luix.utilities.serviceloader.annotation.SpiScope;

@Spi(scope = SpiScope.SINGLETON)
public interface Protocol {

    /**
     * Create provider exporter
     *
     * @param providerUrl provider URL
     * @return exporter
     */
    Exportable export(Url providerUrl);

    /**
     * Create provider invoker
     *
     * @param interfaceName provider interface name
     * @param providerUrl   provider url
     * @return provider invoker
     */
    Sendable createSender(String interfaceName, Url providerUrl);

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
