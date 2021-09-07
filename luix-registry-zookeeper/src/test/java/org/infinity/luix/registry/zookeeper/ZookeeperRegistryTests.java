package org.infinity.luix.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.infinity.luix.core.registry.listener.ClientListener;
import org.infinity.luix.core.registry.listener.CommandListener;
import org.infinity.luix.core.registry.listener.ProviderListener;
import org.infinity.luix.core.switcher.impl.SwitcherHolder;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.registry.zookeeper.service.TestDummyService;
import org.infinity.luix.registry.zookeeper.utils.ZookeeperUtils;
import org.infinity.luix.utilities.annotation.EventMarker;
import org.infinity.luix.utilities.network.AddressUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.*;

import static org.infinity.luix.core.constant.ProtocolConstants.PROTOCOL_VAL_INFINITY;
import static org.infinity.luix.core.constant.RegistryConstants.REGISTRY_VAL_ZOOKEEPER;
import static org.infinity.luix.registry.zookeeper.utils.ZookeeperUtils.FULL_PATH_COMMAND;
import static org.junit.Assert.*;

@Slf4j
public class ZookeeperRegistryTests {
    private static String            REGISTRY_HOST = "127.0.0.1";
    private static ZookeeperRegistry registry;
    private static Url               registryUrl;
    private static Url               providerUrl1;
    private static Url               providerUrl2;
    private static Url               consumerUrl;
    private static EmbeddedZookeeper zookeeper;
    private static ZkClient          zkClient;
    private static String            provider      = TestDummyService.class.getName();

    @BeforeClass
    public static void setUp() throws Exception {
        Properties properties = new Properties();
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        properties.load(in);
        int zkPort = Integer.parseInt(properties.getProperty("clientPort"));
        in.close();

        registryUrl = Url.registryUrl(REGISTRY_VAL_ZOOKEEPER, REGISTRY_HOST, zkPort);
        consumerUrl = Url.consumerUrl(PROTOCOL_VAL_INFINITY, AddressUtils.LOCALHOST, 3000, provider);
        providerUrl1 = Url.providerUrl(PROTOCOL_VAL_INFINITY, AddressUtils.LOCALHOST, 2000, provider);
        providerUrl2 = Url.providerUrl(PROTOCOL_VAL_INFINITY, "192.168.100.100", 2000, provider);

        zookeeper = new EmbeddedZookeeper();
        zookeeper.start();
        Thread.sleep(1000);
        zkClient = new ZkClient(REGISTRY_HOST + ":" + zkPort, 5000);
        registry = new ZookeeperRegistry(zkClient, registryUrl);

        // Delete old data
        zkClient.deleteRecursive(ZookeeperUtils.NAMESPACE);
    }

    @After
    public void tearDown() {
        zkClient.deleteRecursive(ZookeeperUtils.NAMESPACE);
    }

    @Test
    public void testRegisterAndActivate() {
        String node = providerUrl1.getAddress();
        List<String> activateAddrFiles;
        List<String> deactivateAddrFiles;

        String inactivePath = ZookeeperUtils.getStatusDirPath(providerUrl1.getPath(), StatusDir.INACTIVE);
        log.debug("inactivePath: {}", inactivePath);
        String activePath = ZookeeperUtils.getStatusDirPath(providerUrl1.getPath(), StatusDir.ACTIVE);
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
        List<Url> activeProviderUrls = registry.discoverActiveProviders(consumerUrl);
        assertTrue(activeProviderUrls.isEmpty());

        registry.doActivate(providerUrl1);
        registry.doActivate(providerUrl2);
        activeProviderUrls = registry.discoverActiveProviders(consumerUrl);
        assertEquals(2, activeProviderUrls.size());
        assertTrue(activeProviderUrls.contains(providerUrl1));
        assertTrue(activeProviderUrls.contains(providerUrl2));
    }

    @Test
    public void testDiscoverCommand() {
        String result = registry.readCommand(consumerUrl);
        assertTrue(StringUtils.isEmpty(result));

        String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        String commandPath = FULL_PATH_COMMAND;
        if (!zkClient.exists(commandPath)) {
            zkClient.createPersistent(commandPath, true);
        }
        // Write command to zookeeper node
        zkClient.writeData(commandPath, command);

        result = registry.readCommand(consumerUrl);
        Assert.assertEquals(command, result);
    }

    @Test
    @EventMarker
    public void testSubscribeServiceListener() throws Exception {
        ProviderListener providerListener = (refUrl, registryUrl, urls) -> {
            if (CollectionUtils.isNotEmpty(urls)) {
                assertTrue(urls.contains(providerUrl1));
            }
        };
        registry.subscribeProviderListener(consumerUrl, providerListener);
        assertTrue(containsServiceListener(consumerUrl, providerListener));

        registry.doRegister(providerUrl1);
        // Add provider url to zookeeper active node, so provider list changes will trigger the IZkChildListener
        registry.doActivate(providerUrl1);
        Thread.sleep(2000);

        registry.unsubscribeProviderListener(consumerUrl, providerListener);
        assertFalse(containsServiceListener(consumerUrl, providerListener));
    }

    private boolean containsServiceListener(Url consumerUrl, ProviderListener providerListener) {
        return registry.getProviderListenersPerConsumerUrl().get(consumerUrl).containsKey(providerListener);
    }

    @Test
    @EventMarker
    public void testSubscribeCommandListener() throws Exception {
        String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        CommandListener commandListener = (consumerUrl, commandString) -> {
            if (StringUtils.isNotEmpty(commandString)) {
                assertTrue(commandString.equals(command));
            }
        };
        registry.subscribeCommandListener(consumerUrl, commandListener);
        assertTrue(containsCommandListener(consumerUrl, commandListener));

        String commandPath = FULL_PATH_COMMAND;
        if (!zkClient.exists(commandPath)) {
            zkClient.createPersistent(commandPath, true);
        }
        // Write command to zookeeper node, so command list changes will trigger the IZkDataListener
        zkClient.writeData(commandPath, command);
        Thread.sleep(2000);

        zkClient.delete(commandPath);

        registry.unsubscribeCommandListener(consumerUrl, commandListener);
        assertFalse(containsCommandListener(consumerUrl, commandListener));
    }

    private boolean containsCommandListener(Url consumerUrl, CommandListener commandListener) {
        return registry.getCommandListenersPerConsumerUrl().get(consumerUrl).containsKey(commandListener);
    }

    @Test
    @EventMarker
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
        registry.subscribe(consumerUrl, clientListener);
        assertTrue(containsSubscribeListener(consumerUrl, clientListener));

        Thread.sleep(2000);

        String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        String commandPath = FULL_PATH_COMMAND;
        if (!zkClient.exists(commandPath)) {
            zkClient.createPersistent(commandPath, true);
        }
        // Write command to zookeeper node, so command list changes will trigger the IZkDataListener
        zkClient.writeData(commandPath, command);
        Thread.sleep(2000);

        registry.unsubscribe(consumerUrl, clientListener);
        assertFalse(containsSubscribeListener(consumerUrl, clientListener));
    }

    private boolean containsSubscribeListener(Url consumerUrl, ClientListener clientListener) {
        return registry.getCommandServiceListenerPerConsumerUrl().get(consumerUrl).getClientListeners().contains(clientListener);
    }

    @Test
    public void testReregister() throws InterruptedException {
        String node = providerUrl1.getAddress();
        List<String> activateAddrFiles;
        List<String> deactivateAddrFiles;

        String inactivePath = ZookeeperUtils.getStatusDirPath(providerUrl1.getPath(), StatusDir.INACTIVE);
        log.debug("inactivePath: {}", inactivePath);
        String activePath = ZookeeperUtils.getStatusDirPath(providerUrl1.getPath(), StatusDir.ACTIVE);
        log.debug("activePath: {}", activePath);

        registry.register(providerUrl1);
        SwitcherHolder.getInstance().setValue(SwitcherHolder.SERVICE_ACTIVE, true);
        activateAddrFiles = zkClient.getChildren(activePath);
        deactivateAddrFiles = zkClient.getChildren(inactivePath);

//        Thread.sleep(10000L);
        registry.reregisterProviders();

        activateAddrFiles = zkClient.getChildren(activePath);
        deactivateAddrFiles = zkClient.getChildren(inactivePath);

        assertTrue(activateAddrFiles.contains(node));
        assertTrue(deactivateAddrFiles.isEmpty());
    }

    /**
     * 模拟ZookeeperRegistry.doActivate()和ZookeeperRegistry.reregisterProviders()
     */
    @Test
    public void testCollectionElementChange() {
        Set<Url> set1 = new HashSet<>();
        // add a element to set
        set1.add(providerUrl1);
        assertTrue(set1.contains(providerUrl1));

        // then modify the element
        providerUrl1.addOption(Url.PARAM_ACTIVATED_TIME, DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));

        // the set does not contain the element any more
        assertFalse(set1.contains(providerUrl1));
    }
}