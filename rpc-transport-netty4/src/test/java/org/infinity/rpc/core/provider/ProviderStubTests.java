package org.infinity.rpc.core.provider;

import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.config.ProviderConfig;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.constant.ProtocolConstants;
import org.infinity.rpc.core.provider.service.TestService;
import org.infinity.rpc.core.provider.service.impl.TestServiceImpl;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.utilities.network.AddressUtils;
import org.junit.Test;

public class ProviderStubTests {
    @Test
    public void testRegisterProvider() {
        ProviderStub providerStub = new ProviderStub();
        providerStub.setInterfaceClass(TestService.class);
        providerStub.setInterfaceName(TestService.class.getName());
        providerStub.setInstance(new TestServiceImpl());
        providerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        providerStub.setGroup("test");
        providerStub.setVersion("1.0.0");
        providerStub.init();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("Test");
        applicationConfig.setDescription("Description");
        applicationConfig.setTeam("Team");
        applicationConfig.setOwnerMail("test@126.com");
        applicationConfig.setEnv("test");
        applicationConfig.init();

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setPort(2010);
        protocolConfig.init();

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setName("zookeeper");
        registryConfig.setHost("localhost");
        registryConfig.setPort(2181);
        registryConfig.init();

        ProviderConfig providerConfig = new ProviderConfig();

        providerStub.register(applicationConfig, protocolConfig, registryConfig, providerConfig);
    }
}