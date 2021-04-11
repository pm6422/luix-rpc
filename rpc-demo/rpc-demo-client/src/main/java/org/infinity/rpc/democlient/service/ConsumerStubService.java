package org.infinity.rpc.democlient.service;


import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.url.Url;

public interface ConsumerStubService {
    /**
     * Create or get consumer stub
     *
     * @param registryIdentity registry url identity
     * @param providerUrl      provider url
     * @return consumer stub
     */
    ConsumerStub<?> getConsumerStub(String registryIdentity, Url providerUrl);
}
