package org.infinity.rpc.registry.zookeeper;

import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.core.registry.Registrable;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.registry.zookeeper.utils.ZkUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class ZookeeperRegistryTest {
    private static ZookeeperRegistry registry;
    private static Url               serviceUrl, clientUrl;
    private static EmbeddedZookeeper zookeeper;
    private static ZkClient          zkClient;
    private static String            service = "org.infinity.app.common.service.AppService";

    @BeforeClass
    public static void setUp() throws Exception {
        Properties properties = new Properties();
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        properties.load(in);
        int port = Integer.parseInt(properties.getProperty("clientPort"));
        in.close();

        Url zkUrl = Url.of("zookeeper", "127.0.0.1", port, Registrable.class.getName());
        clientUrl = Url.of("infinity", "127.0.0.1", 0, service);
        clientUrl.addParameter("group", "aaa");

        serviceUrl = Url.of("zookeeper", "127.0.0.1", 8001, service);
        serviceUrl.addParameter("group", "aaa");

        zookeeper = new EmbeddedZookeeper();
        zookeeper.start();
        Thread.sleep(1000);
        zkClient = new ZkClient("127.0.0.1:" + port, 5000);
        registry = new ZookeeperRegistry(zkUrl, zkClient);
    }

    @After
    public void tearDown() {
        zkClient.deleteRecursive(ZkUtils.ZOOKEEPER_REGISTRY_NAMESPACE);
    }

    @Test
    public void subAndUnsubService() throws Exception {
        ServiceListener serviceListener = (refUrl, registryUrl, urls) -> {
            if (!urls.isEmpty()) {
                Assert.assertTrue(urls.contains(serviceUrl));
            }
        };
        registry.subscribeService(clientUrl, serviceListener);
        Assert.assertTrue(containsServiceListener(clientUrl, serviceListener));
        registry.doRegister(serviceUrl);
        registry.doActivate(serviceUrl);
        Thread.sleep(2000);

        registry.unsubscribeService(clientUrl, serviceListener);
        Assert.assertFalse(containsServiceListener(clientUrl, serviceListener));
    }

    private boolean containsServiceListener(Url clientUrl, ServiceListener serviceListener) {
        return registry.getServiceListeners().get(clientUrl).containsKey(serviceListener);
    }

    @Test
    public void subAndUnsubCommand() throws Exception {
        final String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
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

    @Test
    public void discoverService() throws Exception {
        registry.doRegister(serviceUrl);
        List<Url> results = registry.discoverService(clientUrl);
        Assert.assertTrue(results.isEmpty());

        registry.doActivate(serviceUrl);
        results = registry.discoverService(clientUrl);
        Assert.assertTrue(results.contains(serviceUrl));
    }

    @Test
    public void discoverCommand() throws Exception {
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
    public void doRegisterAndAvailable() throws Exception {
        String node = serviceUrl.getServerPortStr();
        List<String> available, unavailable;
        String unavailablePath = ZkUtils.toNodeTypePath(serviceUrl, ZkNodeType.INACTIVE_SERVER);
        String availablePath = ZkUtils.toNodeTypePath(serviceUrl, ZkNodeType.ACTIVE_SERVER);

        registry.doRegister(serviceUrl);
        unavailable = zkClient.getChildren(unavailablePath);
        Assert.assertTrue(unavailable.contains(node));

        registry.doActivate(serviceUrl);
        unavailable = zkClient.getChildren(unavailablePath);
        Assert.assertFalse(unavailable.contains(node));
        available = zkClient.getChildren(availablePath);
        Assert.assertTrue(available.contains(node));

        registry.doDeactivate(serviceUrl);
        unavailable = zkClient.getChildren(unavailablePath);
        Assert.assertTrue(unavailable.contains(node));
        available = zkClient.getChildren(availablePath);
        Assert.assertFalse(available.contains(node));

        registry.doUnregister(serviceUrl);
        unavailable = zkClient.getChildren(unavailablePath);
        Assert.assertFalse(unavailable.contains(node));
        available = zkClient.getChildren(availablePath);
        Assert.assertFalse(available.contains(node));
    }

}