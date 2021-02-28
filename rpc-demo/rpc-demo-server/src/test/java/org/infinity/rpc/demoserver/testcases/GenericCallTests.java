package org.infinity.rpc.demoserver.testcases;

import org.infinity.rpc.core.client.invocationhandler.GenericCallHandler;
import org.infinity.rpc.core.client.proxy.ProxyFactory;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.constant.ProtocolConstants;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.switcher.impl.SwitcherService;
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

import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.core.constant.ServiceConstants.CHECK_HEALTH_FACTORY_VAL_DEFAULT;
import static org.junit.Assert.assertEquals;

public class GenericCallTests extends ZkBaseTest {

    private static final int    PROVIDER_PORT = 2001;
    private static final int    CLIENT_PORT   = 2002;
    private static final String GROUP         = GenericCallTests.class.getSimpleName();

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
    public void testGenericCallByRegistry() {
        registerProvider();

        ConsumerStub<?> consumerStub = createConsumerStub(TestService.class.getName());
        ProxyFactory proxyFactory = ProxyFactory.getInstance(PROXY_FACTORY_VAL_JDK);
        GenericCallHandler genericCallHandler = proxyFactory.createGenericCallHandler(consumerStub);
        Map<String, Object> appMap = new HashMap<>();
        appMap.put("name", "testApp");
        appMap.put("enabled", true);
        // Save app first
        genericCallHandler.call("save", new String[]{"org.infinity.rpc.demoserver.service.App"},
                new Object[]{appMap}, new HashMap<>());
        // Then find
        List<App> results = (List<App>) genericCallHandler.call("findAll", null, null, new HashMap<>());
        assertEquals(1, results.size());
    }

    private void registerProvider() {
        ProviderStub<TestService> providerStub = new ProviderStub<>();
        providerStub.setInterfaceClass(TestService.class);
        providerStub.setInterfaceName(TestService.class.getName());
        providerStub.setInstance(new TestServiceImpl());
        providerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        providerStub.setGroup(GROUP);
        providerStub.setVersion("1.0.0");
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
        SwitcherService.getInstance().setValue(SwitcherService.REGISTRY_HEARTBEAT_SWITCHER, true);
    }

    private ConsumerStub<?> createConsumerStub(String interfaceName) {
        ConsumerStub<?> consumerStub = new ConsumerStub<>();
        consumerStub.setInterfaceName(interfaceName);
        consumerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        consumerStub.setCluster(CLUSTER_VAL_DEFAULT);
        consumerStub.setFaultTolerance(FAULT_TOLERANCE_VAL_FAILOVER);
        consumerStub.setLoadBalancer(LOAD_BALANCER_VAL_RANDOM);
        consumerStub.setGroup(GROUP);
        consumerStub.setVersion("1.0.0");
        consumerStub.setProxyFactory(PROXY_FACTORY_VAL_JDK);
        consumerStub.setCheckHealthFactory(CHECK_HEALTH_FACTORY_VAL_DEFAULT);
        // must NOT call init
//        consumerStub.init();

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
        registryConfig.setName("zookeeper");
        registryConfig.setHost("localhost");
        registryConfig.setPort(zkPort);
        registryConfig.init();

        consumerStub.subscribeProviders(applicationConfig, protocolConfig, registryConfig);
        return consumerStub;
    }
}