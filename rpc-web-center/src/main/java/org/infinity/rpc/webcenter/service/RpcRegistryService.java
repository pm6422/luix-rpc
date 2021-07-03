package org.infinity.rpc.webcenter.service;

import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.webcenter.dto.RpcRegistryDTO;

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
     * @param providerUrl      provider url
     * @param interfaceName    interface name
     * @param attributes       consumer stub attributes map
     * @return consumer stub
     */
    ConsumerStub<?> getConsumerStub(String registryIdentity, Url providerUrl, String interfaceName, Map<String, String> attributes);
}
