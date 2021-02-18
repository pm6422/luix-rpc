package org.infinity.rpc.core.network;

import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

@Spi(scope = SpiScope.SINGLETON)
public interface LocalAddressFactory {

    /**
     * Get valid local IP address
     *
     * @return local IP address
     */
    String getLocalAddress();

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static LocalAddressFactory getInstance(String name) {
        return ServiceLoader.forClass(LocalAddressFactory.class).load(name);
    }
}
