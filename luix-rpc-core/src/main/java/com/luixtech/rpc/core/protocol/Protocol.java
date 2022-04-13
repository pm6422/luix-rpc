package com.luixtech.rpc.core.protocol;

import com.luixtech.rpc.core.client.sender.Sendable;
import com.luixtech.rpc.core.server.exposer.ProviderExposable;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.utilities.serviceloader.ServiceLoader;
import com.luixtech.utilities.serviceloader.annotation.Spi;
import com.luixtech.utilities.serviceloader.annotation.SpiScope;

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
