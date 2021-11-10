package org.infinity.luix.webcenter.service;

import org.infinity.luix.core.client.stub.ConsumerStub;
import org.infinity.luix.core.config.impl.RegistryConfig;
import org.infinity.luix.core.registry.Registry;
import org.infinity.luix.webcenter.dto.RpcRegistryDTO;

import java.util.List;
import java.util.Map;

public interface RpcRegistryService {

    List<RpcRegistryDTO> getRegistries();

    Registry findRegistry(String urlIdentity);

    RegistryConfig findRegistryConfig(String urlIdentity);

    /**
     * Create or get consumer stub
     *
     * @param registryIdentity registry url identity
     * @param providerUrlStr   provider url string
     * @param interfaceName    interface name
     * @param attributes       consumer stub attributes map
     * @return consumer stub
     */
    ConsumerStub<?> getConsumerStub(String registryIdentity, String providerUrlStr, String interfaceName, Map<String, String> attributes);
}
