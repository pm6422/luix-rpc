package org.infinity.rpc.core.network.impl;

import org.infinity.rpc.core.exception.impl.RpcConfigException;
import org.infinity.rpc.core.network.LocalIpFactory;
import org.infinity.rpc.utilities.network.IpUtils;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import java.util.Optional;

import static org.infinity.rpc.core.constant.ProtocolConstants.LOCAL_IP_FACTORY_VAL_DEFAULT;

@SpiName(LOCAL_IP_FACTORY_VAL_DEFAULT)
public class DefaultLocalIpFactory implements LocalIpFactory {
    /**
     * Get valid local IP
     *
     * @return local IP
     */
    @Override
    public String getLocalIp() {
        return Optional.ofNullable(IpUtils.getLocalIp()).orElseThrow(() -> new RpcConfigException("Failed to get valid local IP!"));
    }
}
