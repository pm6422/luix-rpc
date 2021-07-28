package org.infinity.rpc.demoserver.testcases;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.config.impl.ApplicationConfig;
import org.infinity.rpc.core.config.impl.ProtocolConfig;
import org.infinity.rpc.core.config.impl.RegistryConfig;
import org.infinity.rpc.core.constant.ProtocolConstants;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.switcher.impl.SwitcherHolder;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.demoserver.service.RefreshUrlService;
import org.infinity.rpc.demoserver.service.impl.RefreshUrlServiceImpl;
import org.infinity.rpc.demoserver.testcases.base.ZkBaseTest;
import org.infinity.rpc.registry.zookeeper.StatusDir;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.infinity.rpc.core.constant.ConsumerConstants.*;
import static org.infinity.rpc.core.constant.ServiceConstants.REQUEST_TIMEOUT;
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
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setName("zookeeper");
        registryConfig.setHost("localhost");
        registryConfig.setPort(zkPort);
        registryConfig.init();

        ProviderStub<RefreshUrlService> providerStub = new ProviderStub<>();
        // Register once
        registerProvider(registryConfig, providerStub, 100);

        List<Url> providerUrls = ZookeeperUtils.readUrls(zkClient, providerStub.getUrl().getPath(), StatusDir.ACTIVE);
        assertEquals("100", providerUrls.get(0).getOption(REQUEST_TIMEOUT));

        // Unregister
        providerStub.unregister(registryConfig.getRegistryUrl());

        // Register twice
        registerProvider(registryConfig, providerStub, 200);

        providerUrls = ZookeeperUtils.readUrls(zkClient, providerStub.getUrl().getPath(), StatusDir.ACTIVE);
        assertEquals("200", providerUrls.get(0).getOption(REQUEST_TIMEOUT));
    }

    private void registerProvider(RegistryConfig registryConfig, ProviderStub<RefreshUrlService> providerStub, int requestTimeout) {
        providerStub.setInterfaceClass(RefreshUrlService.class);
        providerStub.setInterfaceName(RefreshUrlService.class.getName());
        providerStub.setInstance(new RefreshUrlServiceImpl());
        providerStub.setForm(RefreshUrlTests.class.getSimpleName());
        providerStub.setVersion("1.0.0");
        providerStub.setRequestTimeout(requestTimeout);
        providerStub.init();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("server");
        applicationConfig.setDescription("Description");
        applicationConfig.setTeam("Team");
        applicationConfig.setOwnerMail("test@126.com");
        applicationConfig.setMailSuffixes(Arrays.asList("126.com"));
        applicationConfig.setEnv("test");
        applicationConfig.init();

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setPort(PROVIDER_PORT);
        protocolConfig.init();

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
        consumerStub.setForm(RefreshUrlTests.class.getSimpleName());
        consumerStub.setVersion("1.0.0");
        consumerStub.init();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("client");
        applicationConfig.setDescription("Description");
        applicationConfig.setTeam("Team");
        applicationConfig.setOwnerMail("test@126.com");
        applicationConfig.setMailSuffixes(Arrays.asList("126.com"));
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