package org.infinity.rpc.core.network.impl;

import org.infinity.rpc.core.exception.RpcConfigurationException;
import org.infinity.rpc.core.network.LocalAddressFactory;
import org.infinity.rpc.utilities.network.AddressUtils;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import java.util.Optional;

import static org.infinity.rpc.core.constant.ProtocolConstants.LOCAL_ADDRESS_FACTORY_VAL_DEFAULT;

@SpiName(LOCAL_ADDRESS_FACTORY_VAL_DEFAULT)
public class DefaultLocalAddressFactory implements LocalAddressFactory {
    /**
     * Get valid local IP address
     *
     * @return local IP address
     */
    @Override
    public String getLocalAddress() {
        return Optional.ofNullable(AddressUtils.getLocalAddress()).orElseThrow(() -> new RpcConfigurationException("Failed to get valid local IP address!"));
    }
}
