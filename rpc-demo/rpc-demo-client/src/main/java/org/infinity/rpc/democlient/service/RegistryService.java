package org.infinity.rpc.democlient.service;

import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.democlient.dto.RegistryDTO;

import java.util.List;

public interface RegistryService {

    List<RegistryDTO> getRegistries();

    Registry findRegistry(String urlIdentity);

    RegistryConfig findRegistryConfig(String urlIdentity);

    /**
     * Create or get consumer stub
     *
     * @param registryIdentity registry url identity
     * @param providerUrl      provider url
     * @return consumer stub
     */
    ConsumerStub<?> getConsumerStub(String registryIdentity, Url providerUrl);
}
