package org.infinity.luix.registry.consul;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.registry.AbstractRegistryFactory;
import org.infinity.luix.core.registry.Registry;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.registry.consul.client.AbstractConsulClient;
import org.infinity.luix.registry.consul.client.ConsulEcwidClient;
import org.infinity.luix.utilities.serviceloader.annotation.SpiName;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.infinity.luix.core.constant.RegistryConstants.REGISTRY_VAL_CONSUL;

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
        AbstractConsulClient client = new ConsulEcwidClient(host, port);
        return new ConsulRegistry(registryUrl, client);
    }
}
