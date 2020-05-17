package org.infinity.rpc.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.registry.Registrable;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.registry.zookeeper.service.TestDummyService;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.infinity.rpc.utilities.annotation.Event;
import org.infinity.rpc.utilities.network.NetworkIpUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

@Slf4j
public class ZookeeperRegistryTests {
    private static String            REGISTRY_HOST = "127.0.0.1";
    private static ZookeeperRegistry registry;
    private static Url               registryUrl;
    private static Url               providerUrl1;
    private static Url               providerUrl2;
    private static Url               clientUrl;
    private static EmbeddedZookeeper zookeeper;
    private static ZkClient          zkClient;
    private static String            provider      = TestDummyService.class.getName();

    @BeforeClass
    public static void setUp() throws Exception {
        Properties properties = new Properties();
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        properties.load(in);
        int port = Integer.parseInt(properties.getProperty("clientPort"));
        in.close();

        registryUrl = Url.of("zookeeper", REGISTRY_HOST, port, Registrable.class.getName());
        registryUrl.addParameter(Url.PARAM_CONNECT_TIMEOUT, new InfinityProperties().getRegistry().getConnectTimeout().toString());
        registryUrl.addParameter(Url.PARAM_SESSION_TIMEOUT, new InfinityProperties().getRegistry().getSessionTimeout().toString());
        registryUrl.addParameter(Url.PARAM_RETRY_INTERVAL, new InfinityProperties().getRegistry().getRetryInterval().toString());

        // client url use the provider path, but port is 0
        clientUrl = Url.of("infinity", NetworkIpUtils.INTRANET_IP, 0, provider);
        clientUrl.addParameter("group", Url.PARAM_GROUP_DEFAULT_VALUE);

        providerUrl1 = Url.of("infinity", NetworkIpUtils.INTRANET_IP, 2000, provider);
        providerUrl1.addParameter("group", Url.PARAM_GROUP_DEFAULT_VALUE);

        providerUrl2 = Url.of("infinity", "192.168.100.100", 2000, provider);
        providerUrl2.addParameter("group", Url.PARAM_GROUP_DEFAULT_VALUE);

        zookeeper = new EmbeddedZookeeper();
        zookeeper.start();
        Thread.sleep(1000);
        zkClient = new ZkClient(REGISTRY_HOST + ":" + port, 5000);
        registry = new ZookeeperRegistry(registryUrl, zkClient);

        // Delete old data
        zkClient.deleteRecursive(ZookeeperUtils.ZOOKEEPER_REGISTRY_NAMESPACE);
    }

    @After
    public void tearDown() {
        zkClient.deleteRecursive(ZookeeperUtils.ZOOKEEPER_REGISTRY_NAMESPACE);
    }

    @Test
    public void testRegisterAndActivate() {
        String node = providerUrl1.getServerPortStr();
        List<String> activateAddrFiles;
        List<String> deactivateAddrFiles;

        String inactivePath = ZookeeperUtils.getActiveNodePath(providerUrl1, ZookeeperStatusNode.INACTIVE);
        log.debug("inactivePath: {}", inactivePath);
        String activePath = ZookeeperUtils.getActiveNodePath(providerUrl1, ZookeeperStatusNode.ACTIVE);
        log.debug("activePath: {}", activePath);

        registry.doRegister(providerUrl1);
        deactivateAddrFiles = zkClient.getChildren(inactivePath);
        assertTrue(deactivateAddrFiles.contains(node));

        registry.doActivate(providerUrl1);
        deactivateAddrFiles = zkClient.getChildren(inactivePath);
        assertFalse(deactivateAddrFiles.contains(node));
        activateAddrFiles = zkClient.getChildren(activePath);
        assertTrue(activateAddrFiles.contains(node));

        registry.doDeactivate(providerUrl1);
        deactivateAddrFiles = zkClient.getChildren(inactivePath);
        assertTrue(deactivateAddrFiles.contains(node));
        activateAddrFiles = zkClient.getChildren(activePath);
        assertFalse(activateAddrFiles.contains(node));

        registry.doUnregister(providerUrl1);
        deactivateAddrFiles = zkClient.getChildren(inactivePath);
        assertFalse(deactivateAddrFiles.contains(node));
        activateAddrFiles = zkClient.getChildren(activePath);
        assertFalse(activateAddrFiles.contains(node));
    }

    @Test
    public void testDiscoverProviders() throws InterruptedException {
        registry.doRegister(providerUrl1);
        registry.doRegister(providerUrl2);
        List<Url> activeProviderUrls = registry.discoverActiveProviders(clientUrl);
        assertTrue(activeProviderUrls.isEmpty());

        registry.doActivate(providerUrl1);
        registry.doActivate(providerUrl2);
        activeProviderUrls = registry.discoverActiveProviders(clientUrl);
        assertEquals(2, activeProviderUrls.size());
        assertTrue(activeProviderUrls.contains(providerUrl1));
        assertTrue(activeProviderUrls.contains(providerUrl2));
    }

    @Test
    public void testDiscoverCommand() {
        String result = registry.readCommand(clientUrl);
        assertTrue(StringUtils.isEmpty(result));

        String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        String commandPath = ZookeeperUtils.getCommandPath(clientUrl);
        if (!zkClient.exists(commandPath)) {
            zkClient.createPersistent(commandPath, true);
        }
        // Write command to zookeeper node
        zkClient.writeData(commandPath, command);

        result = registry.readCommand(clientUrl);
        Assert.assertEquals(command, result);
    }

    @Test
    @Event
    public void testSubscribeServiceListener() throws Exception {
        ServiceListener serviceListener = (refUrl, registryUrl, urls) -> {
            if (CollectionUtils.isNotEmpty(urls)) {
                assertTrue(urls.contains(providerUrl1));
            }
        };
        registry.subscribeServiceListener(clientUrl, serviceListener);
        assertTrue(containsServiceListener(clientUrl, serviceListener));

        registry.doRegister(providerUrl1);
        // Add provider url to zookeeper active node, so provider list changes will trigger the IZkChildListener
        registry.doActivate(providerUrl1);
        Thread.sleep(2000);

        registry.unsubscribeServiceListener(clientUrl, serviceListener);
        assertFalse(containsServiceListener(clientUrl, serviceListener));
    }

    private boolean containsServiceListener(Url clientUrl, ServiceListener serviceListener) {
        return registry.getProviderListenersPerClientUrl().get(clientUrl).containsKey(serviceListener);
    }

    @Test
    @Event
    public void testSubscribeCommandListener() throws Exception {
        String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        CommandListener commandListener = (clientUrl, commandString) -> {
            if (StringUtils.isNotEmpty(commandString)) {
                assertTrue(commandString.equals(command));
            }
        };
        registry.subscribeCommandListener(clientUrl, commandListener);
        assertTrue(containsCommandListener(clientUrl, commandListener));

        String commandPath = ZookeeperUtils.getCommandPath(clientUrl);
        if (!zkClient.exists(commandPath)) {
            zkClient.createPersistent(commandPath, true);
        }
        // Write command to zookeeper node, so command list changes will trigger the IZkDataListener
        zkClient.writeData(commandPath, command);
        Thread.sleep(2000);

        zkClient.delete(commandPath);

        registry.unsubscribeCommandListener(clientUrl, commandListener);
        assertFalse(containsCommandListener(clientUrl, commandListener));
    }

    private boolean containsCommandListener(Url clientUrl, CommandListener commandListener) {
        return registry.getCommandListenersPerClientUrl().get(clientUrl).containsKey(commandListener);
    }

    @Test
    @Event
    public void testSubscribe() throws InterruptedException {
        registry.doRegister(providerUrl1);
        // Add provider url to zookeeper active node, so provider list changes will trigger the IZkChildListener
        registry.doActivate(providerUrl1);

        ClientListener clientListener = (registryUrl, providerUrls) -> {
            if (CollectionUtils.isNotEmpty(providerUrls)) {
                assertTrue(providerUrls.contains(providerUrl1));
            }
        };
        // subscribe = subscribeServiceListener + subscribeCommandListener+ execute the clientListener
        registry.subscribe(clientUrl, clientListener);
        assertTrue(containsSubscribeListener(clientUrl, clientListener));

        Thread.sleep(2000);

        String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        String commandPath = ZookeeperUtils.getCommandPath(clientUrl);
        if (!zkClient.exists(commandPath)) {
            zkClient.createPersistent(commandPath, true);
        }
        // Write command to zookeeper node, so command list changes will trigger the IZkDataListener
        zkClient.writeData(commandPath, command);
        Thread.sleep(2000);

        registry.unsubscribe(clientUrl, clientListener);
        assertFalse(containsSubscribeListener(clientUrl, clientListener));
    }

    private boolean containsSubscribeListener(Url clientUrl, ClientListener clientListener) {
        return registry.getCommandServiceListenerPerClientUrl().get(clientUrl).getClientListeners().contains(clientListener);
    }
}