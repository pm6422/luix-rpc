package org.infinity.rpc.core.network;

import org.infinity.rpc.utilities.serviceloader.ServiceLoader;
import org.infinity.rpc.utilities.serviceloader.annotation.Spi;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiScope;

@Spi(scope = SpiScope.SINGLETON)
public interface LocalIpFactory {

    /**
     * Get valid local IP
     *
     * @return local IP
     */
    String getLocalIp();

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static LocalIpFactory getInstance(String name) {
        return ServiceLoader.forClass(LocalIpFactory.class).load(name);
    }
}
