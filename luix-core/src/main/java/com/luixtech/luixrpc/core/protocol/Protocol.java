package com.luixtech.luixrpc.core.protocol;

import com.luixtech.luixrpc.core.client.sender.Sendable;
import com.luixtech.luixrpc.core.server.exposer.ProviderExposable;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.utilities.serviceloader.ServiceLoader;
import com.luixtech.luixrpc.utilities.serviceloader.annotation.Spi;
import com.luixtech.luixrpc.utilities.serviceloader.annotation.SpiScope;

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
