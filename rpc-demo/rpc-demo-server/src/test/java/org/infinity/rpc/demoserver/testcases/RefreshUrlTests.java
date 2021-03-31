package org.infinity.rpc.demoserver.testcases;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.ApplicationConfig;
import org.infinity.rpc.core.config.ProtocolConfig;
import org.infinity.rpc.core.config.RegistryConfig;
import org.infinity.rpc.core.constant.ProtocolConstants;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.switcher.impl.SwitcherHolder;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.demoserver.service.TestService;
import org.infinity.rpc.demoserver.service.impl.TestServiceImpl;
import org.infinity.rpc.demoserver.testcases.base.ZkBaseTest;
import org.infinity.rpc.registry.zookeeper.ZookeeperStatusNode;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.infinity.rpc.core.client.stub.ConsumerStub.buildConsumerStubBeanName;
import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.core.constant.ServiceConstants.HEALTH_CHECKER_VAL_DEFAULT;
import static org.infinity.rpc.core.constant.ServiceConstants.REQUEST_TIMEOUT;
import static org.infinity.rpc.core.server.stub.ProviderStub.buildProviderStubBeanName;
import static org.junit.Assert.assertEquals;

@Slf4j
public class RefreshUrlTests extends ZkBaseTest {

    private static final int    PROVIDER_PORT = 2001;
    private static final int    CLIENT_PORT   = 2002;
    private static final String GROUP         = RefreshUrlTests.class.getSimpleName();

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
    public void testRefreshProvider() {
        ProviderStub<TestService> providerStub = new ProviderStub<>();
        // Register once
        registerProvider(providerStub, 100);

        String activePath = ZookeeperUtils.getProviderStatusNodePath(providerStub.getUrl().getForm(), providerStub.getUrl().getPath(), ZookeeperStatusNode.ACTIVE);
        List<String> activateAddrFiles = zkClient.getChildren(activePath);
        String filePath = ZookeeperUtils.getProviderStatusNodePath(providerStub.getUrl().getForm(), providerStub.getUrl().getPath(), ZookeeperStatusNode.ACTIVE) + "/" + activateAddrFiles.get(0);

        Url url1 = Url.valueOf(zkClient.readData(filePath));
        assertEquals("100", url1.getOption(REQUEST_TIMEOUT));

        // Register twice
        registerProvider(providerStub, 200);

        Url url2 = Url.valueOf(zkClient.readData(filePath));
        assertEquals("200", url2.getOption(REQUEST_TIMEOUT));
    }

    private void registerProvider(ProviderStub<TestService> providerStub, int requestTimeout) {
        providerStub.setInterfaceClass(TestService.class);
        providerStub.setInterfaceName(TestService.class.getName());
        providerStub.setInstance(new TestServiceImpl());
        providerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        providerStub.setForm(GROUP);
        providerStub.setVersion("1.0.0");
        providerStub.setRequestTimeout(requestTimeout);
        String beanName = buildProviderStubBeanName(providerStub.getInterfaceClass(), providerStub.getForm(), providerStub.getVersion());
        providerStub.setBeanName(beanName);
        providerStub.setExposed(true);
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

    @Test
    public void testRefreshConsumer() {
        ConsumerStub<TestService> consumerStub = new ConsumerStub<>();
        subscribeProvider(consumerStub);
        subscribeProvider(consumerStub);
    }

    private void subscribeProvider(ConsumerStub<TestService> consumerStub) {
        consumerStub.setInterfaceClass(TestService.class);
        consumerStub.setInterfaceName(TestService.class.getName());
        consumerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        consumerStub.setCluster(CLUSTER_VAL_DEFAULT);
        consumerStub.setFaultTolerance(FAULT_TOLERANCE_VAL_FAILOVER);
        consumerStub.setLoadBalancer(LOAD_BALANCER_VAL_RANDOM);
        consumerStub.setForm(GROUP);
        consumerStub.setVersion("1.0.0");
        consumerStub.setProxy(PROXY_VAL_JDK);
        consumerStub.setHealthChecker(HEALTH_CHECKER_VAL_DEFAULT);
        String beanName = buildConsumerStubBeanName(consumerStub.getInterfaceClass(), Collections.emptyMap());
        consumerStub.setBeanName(beanName);
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
    }
}