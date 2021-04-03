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
import org.infinity.rpc.demoserver.service.RefreshUrlService;
import org.infinity.rpc.demoserver.service.impl.RefreshUrlServiceImpl;
import org.infinity.rpc.demoserver.testcases.base.ZkBaseTest;
import org.infinity.rpc.registry.zookeeper.StatusDir;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.core.constant.ServiceConstants.HEALTH_CHECKER_VAL_DEFAULT;
import static org.infinity.rpc.core.constant.ServiceConstants.REQUEST_TIMEOUT;
import static org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils.getProviderFilePath;
import static org.junit.Assert.assertEquals;

@Slf4j
public class RefreshUrlTests extends ZkBaseTest {

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
    public void testRefreshProvider() {
        ProviderStub<RefreshUrlService> providerStub = new ProviderStub<>();
        // Register once
        registerProvider(providerStub, 100);

        List<String> providerFilePath = getProviderFilePath(zkClient, providerStub.getUrl().getPath(), StatusDir.ACTIVE);

        Url url1 = Url.valueOf(zkClient.readData(providerFilePath.get(0)));
        assertEquals("100", url1.getOption(REQUEST_TIMEOUT));

        // Register twice
        registerProvider(providerStub, 200);

        Url url2 = Url.valueOf(zkClient.readData(providerFilePath.get(0)));
        assertEquals("200", url2.getOption(REQUEST_TIMEOUT));
    }

    private void registerProvider(ProviderStub<RefreshUrlService> providerStub, int requestTimeout) {
        providerStub.setInterfaceClass(RefreshUrlService.class);
        providerStub.setInterfaceName(RefreshUrlService.class.getName());
        providerStub.setInstance(new RefreshUrlServiceImpl());
        providerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        providerStub.setForm(RefreshUrlTests.class.getSimpleName());
        providerStub.setVersion("1.0.0");
        providerStub.setRequestTimeout(requestTimeout);
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
        ConsumerStub<RefreshUrlService> consumerStub = new ConsumerStub<>();
        subscribeProvider(consumerStub);
        subscribeProvider(consumerStub);
    }

    private void subscribeProvider(ConsumerStub<RefreshUrlService> consumerStub) {
        consumerStub.setInterfaceClass(RefreshUrlService.class);
        consumerStub.setInterfaceName(RefreshUrlService.class.getName());
        consumerStub.setProtocol(ProtocolConstants.PROTOCOL_VAL_INFINITY);
        consumerStub.setCluster(CLUSTER_VAL_DEFAULT);
        consumerStub.setFaultTolerance(FAULT_TOLERANCE_VAL_FAILOVER);
        consumerStub.setLoadBalancer(LOAD_BALANCER_VAL_RANDOM);
        consumerStub.setForm(RefreshUrlTests.class.getSimpleName());
        consumerStub.setVersion("1.0.0");
        consumerStub.setProxy(PROXY_VAL_JDK);
        consumerStub.setHealthChecker(HEALTH_CHECKER_VAL_DEFAULT);
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