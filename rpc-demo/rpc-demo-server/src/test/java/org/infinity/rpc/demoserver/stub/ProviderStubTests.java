package org.infinity.rpc.demoserver.stub;

import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.constant.ProtocolConstants;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.switcher.impl.SwitcherService;
import org.infinity.rpc.demoserver.EmbeddedZookeeper;
import org.infinity.rpc.demoserver.service.TestService;
import org.infinity.rpc.demoserver.service.impl.TestServiceImpl;
import org.infinity.rpc.registry.zookeeper.ZookeeperStatusNode;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class ProviderStubTests {

    private static String            REGISTRY_HOST = "127.0.0.1";
    private static int               PROVIDER_PORT = 2000;
    private static int               zkPort;
    private static ZkClient          zkClient;
    private static EmbeddedZookeeper zookeeper;

    @BeforeClass
    public static void setUp() throws Exception {
        Properties properties = new Properties();
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        properties.load(in);
        zkPort = Integer.parseInt(properties.getProperty("clientPort"));
        in.close();

        zookeeper = new EmbeddedZookeeper();
        zookeeper.start();
        Thread.sleep(1000);

        zkClient = new ZkClient(REGISTRY_HOST + ":" + zkPort, 5000);

        // Delete old data
        zkClient.deleteRecursive(ZookeeperUtils.REGISTRY_NAMESPACE);
    }

    @After
    public void tearDown() {
        zkClient.deleteRecursive(ZookeeperUtils.REGISTRY_NAMESPACE);
    }

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
        String activePath = ZookeeperUtils.getProviderStatusNodePath(providerStub.getUrl(), ZookeeperStatusNode.ACTIVE);
        List<String> activateAddrFiles = zkClient.getChildren(activePath);
        String node = providerStub.getUrl().getAddress();
        assertTrue(activateAddrFiles.contains(node));
    }
}