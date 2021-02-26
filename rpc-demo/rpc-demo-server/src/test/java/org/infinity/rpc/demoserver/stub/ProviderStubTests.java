package org.infinity.rpc.demoserver.stub;

import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.constant.ProtocolConstants;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.switcher.impl.SwitcherService;
import org.infinity.rpc.demoserver.EmbeddedZookeeper;
import org.infinity.rpc.demoserver.service.TestService;
import org.infinity.rpc.demoserver.service.impl.TestServiceImpl;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.core.constant.ServiceConstants.CHECK_HEALTH_FACTORY_VAL_DEFAULT;
import static org.infinity.rpc.utilities.network.AddressUtils.LOCALHOST;
import static org.junit.Assert.assertEquals;

public class ProviderStubTests {

    private static final String   REGISTRY_HOST = LOCALHOST;
    private static       int      zkPort        = getZkPort();
    private static final int      PROVIDER_PORT = 2001;
    private static final int      CLINET_PORT   = 2002;
    private static       ZkClient zkClient;

    @BeforeClass
    public static void setUp() throws Exception {
        startZookeeper();
        zkClient = new ZkClient(REGISTRY_HOST + ":" + zkPort, 5000);
        cleanup();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void testCall() {
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
        providerStub.setGroup("test");
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

    private TestService subscribeProvider() {
        ConsumerStub<TestService> consumerStub = new ConsumerStub<>();
        consumerStub.setInterfaceClass(TestService.class);
        consumerStub.setInterfaceName(TestService.class.getName());
        consumerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        consumerStub.setCluster(CLUSTER_VAL_DEFAULT);
        consumerStub.setFaultTolerance(FAULT_TOLERANCE_VAL_FAILOVER);
        consumerStub.setLoadBalancer(LOAD_BALANCER_VAL_RANDOM);
        consumerStub.setGroup("test");
        consumerStub.setVersion("1.0.0");
        consumerStub.setProxyFactory(PROXY_FACTORY_VAL_JDK);
        consumerStub.setCheckHealthFactory(CHECK_HEALTH_FACTORY_VAL_DEFAULT);
        consumerStub.init();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("client");
        applicationConfig.setDescription("Description");
        applicationConfig.setTeam("Team");
        applicationConfig.setOwnerMail("test@126.com");
        applicationConfig.setEnv("test");
        applicationConfig.init();

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setPort(CLINET_PORT);
        protocolConfig.init();

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setName("zookeeper");
        registryConfig.setHost("localhost");
        registryConfig.setPort(zkPort);
        registryConfig.init();

        consumerStub.subscribeProviders(applicationConfig, protocolConfig, registryConfig);
        return consumerStub.getProxyInstance();
    }

    private static int getZkPort() {
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        Properties properties = new Properties();
        try {
            properties.load(in);
            in.close();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read zoo.cfg!");
        }
        return Integer.parseInt(properties.getProperty("clientPort"));
    }

    private static void startZookeeper() throws Exception {
        EmbeddedZookeeper zookeeper = new EmbeddedZookeeper();
        zookeeper.start();
        Thread.sleep(500);
    }

    private static void cleanup() {
        zkClient.deleteRecursive(ZookeeperUtils.REGISTRY_NAMESPACE);
    }
}