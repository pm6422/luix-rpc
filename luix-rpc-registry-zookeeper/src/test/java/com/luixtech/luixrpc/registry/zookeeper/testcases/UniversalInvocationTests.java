package com.luixtech.luixrpc.registry.zookeeper.testcases;

import com.luixtech.luixrpc.core.client.invocationhandler.UniversalInvocationHandler;
import com.luixtech.luixrpc.core.client.proxy.Proxy;
import com.luixtech.luixrpc.core.client.stub.ConsumerStub;
import com.luixtech.luixrpc.core.client.stub.ConsumerStubFactory;
import com.luixtech.luixrpc.core.config.impl.ApplicationConfig;
import com.luixtech.luixrpc.core.config.impl.ProtocolConfig;
import com.luixtech.luixrpc.core.config.impl.RegistryConfig;
import com.luixtech.luixrpc.core.server.stub.ProviderStub;
import com.luixtech.luixrpc.registry.zookeeper.service.App;
import com.luixtech.luixrpc.registry.zookeeper.service.TestService;
import com.luixtech.luixrpc.registry.zookeeper.service.impl.TestServiceImpl;
import com.luixtech.luixrpc.registry.zookeeper.testcases.base.ZkBaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.luixtech.luixrpc.core.constant.ConsumerConstants.PROXY_VAL_JDK;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UniversalInvocationTests extends ZkBaseTest {

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
    public void testUniversalCallByRegistry() throws InterruptedException {
        registerProvider();

        ConsumerStub<?> consumerStub = createConsumerStub(TestService.class.getName());
        Proxy proxyFactory = Proxy.getInstance(PROXY_VAL_JDK);
        UniversalInvocationHandler universalInvocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Map<String, Object> appMap = new HashMap<>();
        appMap.put("name", "testApp");
        appMap.put("enabled", true);
        // Save app first
        universalInvocationHandler.invoke("save", new String[]{"com.luixtech.luixrpc.registry.zookeeper.service.App"}, new Object[]{appMap});
        Thread.sleep(100);
        // Then find
        List<App> results = (List<App>) universalInvocationHandler.invoke("findAll", null, null);
        assertEquals(1, results.size());
    }

    private void registerProvider() {
        ProviderStub<TestService> providerStub = new ProviderStub<>();
        providerStub.setInterfaceClass(TestService.class);
        providerStub.setInterfaceName(TestService.class.getName());
        providerStub.setInstance(new TestServiceImpl());
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

    private ConsumerStub<?> createConsumerStub(String interfaceName) {
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
        registryConfig.setHost("localhost");
        registryConfig.setPort(zkPort);
        registryConfig.init();

        return ConsumerStubFactory.create(applicationConfig, registryConfig, protocolConfig, interfaceName);
    }
}