package org.infinity.rpc.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Deprecated
public class ZkRpcServerRegistry {
    private          String       registryAddress;
    private          ZooKeeper    zooKeeper;
    // 所有提供服务的服务器列表
    private volatile List<String> serverList = new ArrayList<>();

    public ZkRpcServerRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public void createRpcServerNode(String data) throws Exception {
        //创建一个客户端程序, 对于注册可以不用监听事件
        zooKeeper = new ZooKeeper(registryAddress, Constant.SESSION_TIMEOUT, event -> {
        });

        if (zooKeeper != null) {
            try {
                // 判断注册的目录是否存在
                Stat stat = zooKeeper.exists(Constant.REGISTRY_PATH, false);
                if (stat == null) {
                    // 如果不存在，创建一个持久节点目录
                    zooKeeper.create(Constant.REGISTRY_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                    log.debug("Created a persistent directory [{}]", Constant.REGISTRY_PATH);
                }
                // 创建一个临时节点用于保存数据信息
                zooKeeper.create(Constant.DATA_PATH, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
                log.debug("Created a temporary data node [{}] on [{}]", data, Constant.DATA_PATH);
            } catch (Exception e) {
                log.error("Failed to create node with error: {}", e.getMessage());
            }
        } else {
            log.error("ZooKeeper connect is null");
        }
    }

    public void startWatchNode() {
        if (zooKeeper == null) {
            try {
                zooKeeper = new ZooKeeper(registryAddress, Constant.SESSION_TIMEOUT, watchedEvent -> {
                    if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                        // 实时监听zkServer的服务器列表变化
                        watchNode();
                    }
                });
            } catch (IOException e) {
                log.error("Failed to register zk with error: {}", e.getMessage());
            }
            // 获取节点相关数据
            watchNode();
        }
    }

    /**
     * 监听服务端的列表信息
     */
    private void watchNode() {
        try {
            //获取子节点信息
            List<String> nodeList = zooKeeper.getChildren(Constant.REGISTRY_PATH, true);
            for (String node : nodeList) {
                byte[] bytes = zooKeeper.getData(Constant.REGISTRY_PATH + "/" + node, false, null);
                serverList.add(new String(bytes));
            }

            if (serverList.size() == 0) {
                log.error("No RPC server found");
            } else {
                log.info("Discovered RPC servers [{}]", serverList);
            }
        } catch (Exception e) {
            log.error("Failed to get nodes with error: {}", e.getMessage());
        }
    }

    public void checkRegisteredRpcServer() {
        log.debug("Checking registered RPC server");
        try {
            //创建一个客户端程序, 对于注册可以不用监听事件
            ZooKeeper zk = new ZooKeeper(registryAddress, Constant.SESSION_TIMEOUT, event -> {
            });
            //获取子节点信息
            List<String> nodeList = zk.getChildren(Constant.REGISTRY_PATH, true);
            List<String> serverList = new ArrayList<>();
            for (String node : nodeList) {
                byte[] bytes = zooKeeper.getData(Constant.REGISTRY_PATH + "/" + node, false, null);
                String server = new String(bytes);
                serverList.add(server);
                log.debug("Found registered RPC server: [{}]", server);
            }

            if (serverList.size() == 0) {
                throw new RuntimeException("No RPC server found");
            }

        } catch (Exception e) {
            log.error("Failed to get nodes with error: {}", e.getMessage());
        }
        log.debug("Checked registered RPC server");

    }


    /**
     * 随机返回一台服务器地址信息，用于负载均衡
     *
     * @return
     */
    public String discoverRpcServer() {
        int size = serverList.size();
        if (size == 0) {
            throw new RuntimeException("No RPC server found");
        }
        int index = new Random().nextInt(size);
        String server = serverList.get(index);
        log.info("Got RPC server [{}] by load balance algorithm", server);
        return server;
    }
}
