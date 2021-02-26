package org.infinity.rpc.demoserver.testcases.base;

import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.demoserver.utils.EmbeddedZookeeper;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;

import java.io.InputStream;
import java.util.Properties;

import static org.infinity.rpc.utilities.network.AddressUtils.LOCALHOST;

public abstract class ZkBaseTest {

    protected static       int      zkPort        = getZkPort();
    protected static final String   REGISTRY_HOST = LOCALHOST;
    protected static       ZkClient zkClient;

    protected static int getZkPort() {
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        Properties properties = new Properties();
        try {
            properties.load(in);
            in.close();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read zoo.cfg!");
        }
        return Integer.parseInt(properties.getProperty("clientPort"));
    }

    protected static void initZkClient() {
        zkClient = new ZkClient(REGISTRY_HOST + ":" + zkPort, 5000);
    }

    protected static void startZookeeper() throws Exception {
        EmbeddedZookeeper zookeeper = new EmbeddedZookeeper();
        zookeeper.start();
        Thread.sleep(500);
    }

    protected static void cleanup() {
        zkClient.deleteRecursive(ZookeeperUtils.REGISTRY_NAMESPACE);
    }

}
