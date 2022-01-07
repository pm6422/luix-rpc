package org.infinity.luix.demoserver.testcases;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.client.stub.ConsumerStub;
import org.infinity.luix.core.config.impl.ApplicationConfig;
import org.infinity.luix.core.config.impl.ProtocolConfig;
import org.infinity.luix.core.config.impl.RegistryConfig;
import org.infinity.luix.core.server.stub.ProviderStub;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.demoserver.service.RefreshUrlService;
import org.infinity.luix.demoserver.service.impl.RefreshUrlServiceImpl;
import org.infinity.luix.demoserver.testcases.base.ZkBaseTest;
import org.infinity.luix.registry.zookeeper.StatusDir;
import org.infinity.luix.registry.zookeeper.utils.ZookeeperUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.infinity.luix.core.constant.ServiceConstants.REQUEST_TIMEOUT;
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
    public void tearDown() {
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
        applicationConfig.setOwnerMail("test@126.com");
        applicationConfig.setMailSuffixes(Arrays.asList("126.com"));
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