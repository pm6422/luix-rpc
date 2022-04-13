package com.luixtech.luixrpc.core.server.buildin;

import com.luixtech.luixrpc.core.client.stub.ConsumerStub;
import com.luixtech.luixrpc.core.client.stub.ConsumerStubFactory;
import com.luixtech.luixrpc.core.client.stub.ConsumerStubHolder;
import com.luixtech.luixrpc.core.config.impl.ApplicationConfig;
import com.luixtech.luixrpc.core.config.impl.ProtocolConfig;
import com.luixtech.luixrpc.core.config.impl.RegistryConfig;

import java.util.HashMap;
import java.util.Map;

import static com.luixtech.luixrpc.core.constant.ConsumerConstants.PROVIDER_ADDRESSES;
import static com.luixtech.luixrpc.core.constant.ServiceConstants.REQUEST_TIMEOUT;
import static com.luixtech.luixrpc.core.constant.ServiceConstants.RETRY_COUNT;

public interface BuildInService {
    String METHOD_GET_APPLICATION_INFO = "getApplicationInfo";
    String METHOD_GET_SERVER_INFO      = "getServerInfo";

    /**
     * Get application information
     *
     * @return application information
     */
    ApplicationConfig getApplicationInfo();

    /**
     * Get server information
     *
     * @return server information
     */
    ServerInfo getServerInfo();

    /**
     * Create consumer stub
     *
     * @param applicationConfig application configuration
     * @param registryConfig    registry configuration
     * @param protocolConfig    protocol configuration
     * @param providerAddresses provider addresses
     * @param requestTimeout    request timeout
     * @param retryCount        retry count
     * @return consumer stub
     */
    static ConsumerStub<?> createConsumerStub(ApplicationConfig applicationConfig,
                                              RegistryConfig registryConfig,
                                              ProtocolConfig protocolConfig,
                                              String providerAddresses,
                                              Integer requestTimeout,
                                              Integer retryCount) {
        Map<String, Object> attributes = new HashMap<>(2);
        attributes.put(PROVIDER_ADDRESSES, providerAddresses);
        attributes.put(REQUEST_TIMEOUT, requestTimeout);
        attributes.put(RETRY_COUNT, retryCount);

        String stubBeanName = ConsumerStub.buildConsumerStubBeanName(BuildInService.class.getName(), attributes);

        if (!ConsumerStubHolder.getInstance().getMap().containsKey(stubBeanName)) {
            ConsumerStub<?> consumerStub = ConsumerStubFactory.create(applicationConfig, registryConfig,
                    protocolConfig, providerAddresses, BuildInService.class.getName(),
                    requestTimeout, retryCount);
            ConsumerStubHolder.getInstance().add(stubBeanName, consumerStub);
        }
        return ConsumerStubHolder.getInstance().getMap().get(stubBeanName);
    }
}
