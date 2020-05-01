package org.infinity.rpc.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.registry.Registrable;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.registry.zookeeper.service.TestDummyService;
import org.infinity.rpc.registry.zookeeper.utils.ZkUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

@Slf4j
public class ZookeeperRegistryTest {
    private static ZookeeperRegistry registry;
    private static Url               registryUrl;
    private static Url               providerUrl;
    private static Url               clientUrl;
    private static EmbeddedZookeeper zookeeper;
    private static ZkClient          zkClient;
    private static String            provider = TestDummyService.class.getName();

    @BeforeClass
    public static void setUp() throws Exception {
        Properties properties = new Properties();
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        properties.load(in);
        int port = Integer.parseInt(properties.getProperty("clientPort"));
        in.close();

        registryUrl = Url.of("zookeeper", "127.0.0.1", port, Registrable.class.getName());
        registryUrl.addParameter(Url.PARAM_CONNECT_TIMEOUT, new InfinityProperties().getRegistry().getConnectTimeout().toString());
        registryUrl.addParameter(Url.PARAM_SESSION_TIMEOUT, new InfinityProperties().getRegistry().getSessionTimeout().toString());
        registryUrl.addParameter(Url.PARAM_RETRY_INTERVAL, new InfinityProperties().getRegistry().getRetryInterval().toString());

        clientUrl = Url.of("infinity", "127.0.0.1", 0, provider);
        clientUrl.addParameter("group", Url.PARAM_GROUP_VALUE);

        providerUrl = Url.of("infinity", "127.0.0.1", 8001, provider);
        providerUrl.addParameter("group", Url.PARAM_GROUP_VALUE);

        zookeeper = new EmbeddedZookeeper();
        zookeeper.start();
        Thread.sleep(1000);
        zkClient = new ZkClient("127.0.0.1:" + port, 5000);
        registry = new ZookeeperRegistry(registryUrl, zkClient);
    }

    @After
    public void tearDown() {
        zkClient.deleteRecursive(ZkUtils.ZOOKEEPER_REGISTRY_NAMESPACE);
    }

    @Test
    public void testRegisterAndActivate() {
        String node = providerUrl.getServerPortStr();
        List<String> activateList;
        List<String> deactivateList;

        String inactivePath = ZkUtils.getPathByNode(providerUrl, ZkNodeType.INACTIVE_SERVER);
        log.debug("inactivePath: {}", inactivePath);
        String activePath = ZkUtils.getPathByNode(providerUrl, ZkNodeType.ACTIVE_SERVER);
        log.debug("activePath: {}", activePath);

        registry.doRegister(providerUrl);
        deactivateList = zkClient.getChildren(inactivePath);
        Assert.assertTrue(deactivateList.contains(node));

        registry.doActivate(providerUrl);
        deactivateList = zkClient.getChildren(inactivePath);
        Assert.assertFalse(deactivateList.contains(node));
        activateList = zkClient.getChildren(activePath);
        Assert.assertTrue(activateList.contains(node));

        registry.doDeactivate(providerUrl);
        deactivateList = zkClient.getChildren(inactivePath);
        Assert.assertTrue(deactivateList.contains(node));
        activateList = zkClient.getChildren(activePath);
        Assert.assertFalse(activateList.contains(node));

        registry.doUnregister(providerUrl);
        deactivateList = zkClient.getChildren(inactivePath);
        Assert.assertFalse(deactivateList.contains(node));
        activateList = zkClient.getChildren(activePath);
        Assert.assertFalse(activateList.contains(node));
    }

    @Test
    public void testDiscover() {
        registry.doRegister(providerUrl);
        List<Url> results = registry.discoverProviders(clientUrl);
        Assert.assertTrue(results.isEmpty());

        registry.doActivate(providerUrl);
        results = registry.discoverProviders(clientUrl);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));
        Assert.assertTrue(results.contains(providerUrl));
    }

    @Test
    public void discoverCommand() {
        String result = registry.discoverCommand(clientUrl);
        Assert.assertTrue(result.equals(""));

        String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        String commandPath = ZkUtils.toCommandPath(clientUrl);
        if (!zkClient.exists(commandPath)) {
            zkClient.createPersistent(commandPath, true);
        }
        zkClient.writeData(commandPath, command);
        result = registry.discoverCommand(clientUrl);
        Assert.assertTrue(result.equals(command));
    }

    @Test
    public void testSubscribe() throws Exception {
        ServiceListener serviceListener = (refUrl, registryUrl, urls) -> {
            if (!urls.isEmpty()) {
                Assert.assertTrue(urls.contains(providerUrl));
            }
        };
        registry.subscribeService(clientUrl, serviceListener);
        Assert.assertTrue(containsServiceListener(clientUrl, serviceListener));
        registry.doRegister(providerUrl);
        registry.doActivate(providerUrl);
        Thread.sleep(2000);

        registry.unsubscribeService(clientUrl, serviceListener);
        Assert.assertFalse(containsServiceListener(clientUrl, serviceListener));
    }

    private boolean containsServiceListener(Url clientUrl, ServiceListener serviceListener) {
        return registry.getServiceListeners().get(clientUrl).containsKey(serviceListener);
    }

    @Test
    public void subAndUnsubCommand() throws Exception {
        String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        CommandListener commandListener = (refUrl, commandString) -> {
            if (commandString != null) {
                Assert.assertTrue(commandString.equals(command));
            }
        };
        registry.subscribeCommand(clientUrl, commandListener);
        Assert.assertTrue(containsCommandListener(clientUrl, commandListener));

        String commandPath = ZkUtils.toCommandPath(clientUrl);
        if (!zkClient.exists(commandPath)) {
            zkClient.createPersistent(commandPath, true);
        }
        zkClient.writeData(commandPath, command);
        Thread.sleep(2000);

        zkClient.delete(commandPath);

        registry.unsubscribeCommand(clientUrl, commandListener);
        Assert.assertFalse(containsCommandListener(clientUrl, commandListener));
    }

    private boolean containsCommandListener(Url clientUrl, CommandListener commandListener) {
        return registry.getCommandListeners().get(clientUrl).containsKey(commandListener);
    }
}