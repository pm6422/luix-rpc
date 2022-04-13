package com.luixtech.luixrpc.registry.zookeeper.testcases;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.luixrpc.core.client.stub.ConsumerStub;
import com.luixtech.luixrpc.core.config.impl.ApplicationConfig;
import com.luixtech.luixrpc.core.config.impl.ProtocolConfig;
import com.luixtech.luixrpc.core.config.impl.RegistryConfig;
import com.luixtech.luixrpc.core.server.stub.ProviderStub;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.registry.zookeeper.service.RefreshUrlService;
import com.luixtech.luixrpc.registry.zookeeper.service.impl.RefreshUrlServiceImpl;
import com.luixtech.luixrpc.registry.zookeeper.StatusDir;
import com.luixtech.luixrpc.registry.zookeeper.testcases.base.ZkBaseTest;
import com.luixtech.luixrpc.registry.zookeeper.utils.ZookeeperUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.luixtech.luixrpc.core.constant.ServiceConstants.REQUEST_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class RefreshUrlTests extends ZkBaseTest {

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
    public void testOverrideProviderOptions() {
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

        // Register twice
        providerStub.reregister(ImmutableMap.of(REQUEST_TIMEOUT, "200"));

        providerUrls = ZookeeperUtils.readUrls(zkClient, providerStub.getUrl().getPath(), StatusDir.ACTIVE);
        assertEquals("200", providerUrls.get(0).getOption(REQUEST_TIMEOUT));
    }

    private void registerProvider(RegistryConfig registryConfig, ProviderStub<RefreshUrlService> providerStub, int requestTimeout) {
        providerStub.setInterfaceClass(RefreshUrlService.class);
        providerStub.setInterfaceName(RefreshUrlService.class.getName());
        providerStub.setInstance(new RefreshUrlServiceImpl());
        providerStub.setRequestTimeout(requestTimeout);
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

        providerStub.register(applicationConfig, protocolConfig, registryConfig);

        // Activate provider
        providerStub.activate();
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
    }
}