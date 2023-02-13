package com.luixtech.rpc.registry.consul;

import com.luixtech.rpc.core.registry.Registry;
import com.luixtech.rpc.core.registry.factory.AbstractRegistryFactory;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.utilities.serviceloader.annotation.SpiName;
import lombok.extern.slf4j.Slf4j;

import static com.luixtech.rpc.core.constant.RegistryConstants.REGISTRY_VAL_CONSUL;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@SpiName(REGISTRY_VAL_CONSUL)
@Slf4j
public class ConsulRegistryFactory extends AbstractRegistryFactory {

    private static final String DEFAULT_HOST = "localhost";
    private static final int    DEFAULT_PORT = 8500;

    /**
     * Create a consul registry
     *
     * @param registryUrl registry URL
     * @return registry instance
     */
    @Override
    public Registry createRegistry(Url registryUrl) {
        String host = defaultIfBlank(registryUrl.getHost(), DEFAULT_HOST);
        int port = registryUrl.getPort() > 0 ? registryUrl.getPort() : DEFAULT_PORT;
        ConsulHttpClient consulClient = new ConsulHttpClient(host, port);
        return new ConsulRegistry(registryUrl, consulClient);
    }
}
