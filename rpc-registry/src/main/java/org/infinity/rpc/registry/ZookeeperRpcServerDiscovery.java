package org.infinity.rpc.registry;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 根据注册中心地址实时发现服务器列表
 */
public class ZookeeperRpcServerDiscovery {
    public static final Logger       LOGGER     = LoggerFactory.getLogger(ZookeeperRpcServerRegistry.class);
    // 注册中心地址
    private             String       registryAddress;
    private             ZooKeeper    zooKeeper;
    // 所有提供服务的服务器列表
    private volatile    List<String> serverList = new ArrayList<>();

    public ZookeeperRpcServerDiscovery(String registryAddress) throws Exception {
        this.registryAddress = registryAddress;
        zooKeeper = new ZooKeeper(registryAddress, Constant.SESSION_TIMEOUT, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                    // 实时监听zkServer的服务器列表变化
                    watchNode();
                }
            }
        });
        // 获取节点相关数据
        watchNode();
    }

    /**
     * 随机返回一台服务器地址信息，用于负载均衡
     *
     * @return
     */
    public String discoverRpcServer() {
        int size = serverList.size();
        if (size == 0) {
            throw new RuntimeException("No server found.");
        }
        int index = new Random().nextInt(size);
        return serverList.get(index);
    }

    /**
     * 监听服务端的列表信息
     */
    public void watchNode() {
        try {
            //获取子节点信息
            List<String> nodeList = zooKeeper.getChildren(Constant.REGISTRY_PATH, true);
            for (String node : nodeList) {
                byte[] bytes = zooKeeper.getData(Constant.REGISTRY_PATH + "/" + node, false, null);
                serverList.add(new String(bytes));
            }

            if (serverList.size() == 0) {
                LOGGER.error("No server list found");
            } else {
                LOGGER.info("Discovered server list: {}", serverList);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get nodes", e);
        }
    }
}
