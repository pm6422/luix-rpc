package org.infinity.rpc.demoserver.testcases;

import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.config.impl.ProtocolConfig;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.constant.ProtocolConstants;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.switcher.impl.SwitcherHolder;
import org.infinity.rpc.demoserver.service.TestService;
import org.infinity.rpc.demoserver.service.impl.TestServiceImpl;
import org.infinity.rpc.demoserver.testcases.base.ZkBaseTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.core.constant.ServiceConstants.HEALTH_CHECKER_VAL_DEFAULT;
import static org.junit.Assert.assertEquals;

public class DirectCallTests extends ZkBaseTest {

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
    public void directInvocation() {
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
        providerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        providerStub.setForm(DirectCallTests.class.getSimpleName());
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
        SwitcherHolder.getInstance().setValue(SwitcherHolder.SERVICE_ACTIVE, true);
    }

    private TestService subscribeProvider() {
        ConsumerStub<TestService> consumerStub = new ConsumerStub<>();
        consumerStub.setInterfaceClass(TestService.class);
        consumerStub.setInterfaceName(TestService.class.getName());
        consumerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        consumerStub.setInvoker(INVOKER_VAL_P2P);
        consumerStub.setFaultTolerance(FAULT_TOLERANCE_VAL_FAILOVER);
        consumerStub.setLoadBalancer(LOAD_BALANCER_VAL_RANDOM);
        consumerStub.setForm(DirectCallTests.class.getSimpleName());
        consumerStub.setVersion("1.0.0");
        consumerStub.setProxy(PROXY_VAL_JDK);
        consumerStub.setHealthChecker(HEALTH_CHECKER_VAL_DEFAULT);
        // Set direct address
        consumerStub.setProviderAddresses("localhost:" + PROVIDER_PORT);
        consumerStub.init();

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
        return consumerStub.getProxyInstance();
    }
}