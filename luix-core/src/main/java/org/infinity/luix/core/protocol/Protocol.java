package org.infinity.luix.core.protocol;

import org.infinity.luix.core.client.sender.Sendable;
import org.infinity.luix.core.server.exposer.ProviderExposable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.serviceloader.ServiceLoader;
import org.infinity.luix.utilities.serviceloader.annotation.Spi;
import org.infinity.luix.utilities.serviceloader.annotation.SpiScope;

@Spi(scope = SpiScope.SINGLETON)
public interface Protocol {

    /**
     * Create RPC request sender
     *
     * @param interfaceName provider interface name
     * @param providerUrl   provider url
     * @return RPC request sender
     */
    Sendable createRequestSender(String interfaceName, Url providerUrl);

    /**
     * Expose provider
     *
     * @param providerUrl provider URL
     * @return exposer
     */
    ProviderExposable exposeProvider(Url providerUrl);

    /**
     * Hide provider
     *
     * @param providerUrl provider URL
     */
    void hideProvider(Url providerUrl);

    /**
     * Destroy providers
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
