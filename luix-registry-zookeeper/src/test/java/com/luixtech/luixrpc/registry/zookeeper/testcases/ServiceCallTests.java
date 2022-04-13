package com.luixtech.luixrpc.registry.zookeeper.testcases;

import com.luixtech.luixrpc.core.client.stub.ConsumerStub;
import com.luixtech.luixrpc.core.config.impl.ApplicationConfig;
import com.luixtech.luixrpc.core.config.impl.ProtocolConfig;
import com.luixtech.luixrpc.core.config.impl.RegistryConfig;
import com.luixtech.luixrpc.core.server.stub.ProviderStub;
import com.luixtech.luixrpc.registry.zookeeper.service.TestService;
import com.luixtech.luixrpc.registry.zookeeper.service.impl.TestServiceImpl;
import com.luixtech.luixrpc.registry.zookeeper.testcases.base.ZkBaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServiceCallTests extends ZkBaseTest {

    private static final int PROVIDER_PORT = 2001;
    private static final int CLIENT_PORT   = 2002;

    @BeforeAll
    public static void setUp() throws Exception {
        startZookeeper();
        initZkClient();
        cleanup();
    }

    @AfterAll
    public static void tearDown() {
        cleanup();
    }

    @Test
    public void testCallByRegistry() {
        registerProvider();
        TestService proxyInstance = subscribeProvider();
        String result = proxyInstance.hello("louis");
        assertEquals("hello louis", result);
    }

    private void registerProvider() {
        ProviderStub<TestService> providerStub = new ProviderStub<>();
        providerStub.setInterfaceClass(TestService.class);
        providerStub.setInterfaceName(TestService.class.getName());
        providerStub.setInstance(new TestServiceImpl());
        providerStub.setForm(ServiceCallTests.class.getSimpleName());
        providerStub.setVersion("1.0.0");
        providerStub.init();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setId("server");
        applicationConfig.setDescription("Description");
        applicationConfig.setTeam("Team");
        applicationConfig.setOwnerEmail("test@126.com");
        applicationConfig.setEmailSuffixes(Arrays.asList("126.com"));
        applicationConfig.setEnv("test");
        applicationConfig.init();

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setPort(PROVIDER_PORT);
        protocolConfig.init();

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setName("zookeeper");
        registryConfig.setHost("localhost");
        registryConfig.setPort(zkPort);
        registryConfig.init();

        providerStub.register(applicationConfig, protocolConfig, registryConfig);

        // Activate provider
        providerStub.activate();
    }

    private TestService subscribeProvider() {
        ConsumerStub<TestService> consumerStub = new ConsumerStub<>();
        consumerStub.setInterfaceClass(TestService.class);
        consumerStub.setInterfaceName(TestService.class.getName());
        consumerStub.setForm(ServiceCallTests.class.getSimpleName());
        consumerStub.setVersion("1.0.0");
        consumerStub.init();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setId("client");
        applicationConfig.setDescription("Description");
        applicationConfig.setTeam("Team");
        applicationConfig.setOwnerEmail("test@126.com");
        applicationConfig.setEmailSuffixes(Arrays.asList("126.com"));
        applicationConfig.setEnv("test");
        applicationConfig.init();

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setPort(CLIENT_PORT);
        protocolConfig.init();

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setName("zookeeper");
        registryConfig.setHost("localhost");
        registryConfig.setPort(zkPort);
        registryConfig.init();

        consumerStub.subscribeProviders(applicationConfig, protocolConfig, registryConfig);
        return consumerStub.getProxyInstance();
    }
}