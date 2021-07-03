package org.infinity.rpc.demoserver.testcases;

import org.infinity.rpc.core.client.invocationhandler.UniversalInvocationHandler;
import org.infinity.rpc.core.client.proxy.Proxy;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.config.impl.ConsumerConfig;
import org.infinity.rpc.core.config.impl.ProtocolConfig;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.constant.ProtocolConstants;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.switcher.impl.SwitcherHolder;
import org.infinity.rpc.demoserver.service.App;
import org.infinity.rpc.demoserver.service.TestService;
import org.infinity.rpc.demoserver.service.impl.TestServiceImpl;
import org.infinity.rpc.demoserver.testcases.base.ZkBaseTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.infinity.rpc.core.constant.ConsumerConstants.PROXY_VAL_JDK;
import static org.junit.Assert.assertEquals;

public class UniversalInvocationTests extends ZkBaseTest {

    private static final int PROVIDER_PORT = 2001;
    private static final int CLIENT_PORT   = 2002;

    @BeforeClass
    public static void setUp() throws Exception {
        startZookeeper();
        initZkClient();
        cleanup();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void testGenericCallByRegistry() throws InterruptedException {
        registerProvider();

        ConsumerStub<?> consumerStub = createConsumerStub(TestService.class.getName());
        Proxy proxyFactory = Proxy.getInstance(PROXY_VAL_JDK);
        UniversalInvocationHandler universalInvocationHandler = proxyFactory.createUniversalInvocationHandler(consumerStub);
        Map<String, Object> appMap = new HashMap<>();
        appMap.put("name", "testApp");
        appMap.put("enabled", true);
        // Save app first
        universalInvocationHandler.invoke("save", new String[]{"org.infinity.rpc.demoserver.service.App"}, new Object[]{appMap});
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
        providerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        providerStub.init();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("server");
        applicationConfig.setDescription("Description");
        applicationConfig.setTeam("Team");
        applicationConfig.setOwnerMail("test@126.com");
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
        SwitcherHolder.getInstance().setValue(SwitcherHolder.SERVICE_ACTIVE, true);
    }

    private ConsumerStub<?> createConsumerStub(String interfaceName) {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("client");
        applicationConfig.setDescription("Description");
        applicationConfig.setTeam("Team");
        applicationConfig.setOwnerMail("test@126.com");
        applicationConfig.setEnv("test");
        applicationConfig.init();

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setPort(CLIENT_PORT);
        protocolConfig.init();

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setHost("localhost");
        registryConfig.setPort(zkPort);
        registryConfig.init();

        ConsumerStub<?> consumerStub = ConsumerStub.create(interfaceName, applicationConfig,
                registryConfig, protocolConfig, new ConsumerConfig(), null);
        return consumerStub;
    }
}