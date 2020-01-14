package org.infinity.rpc.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

@Slf4j
public class ZkRpcServerRegistry {
    private             String    registryAddress;
    private             ZooKeeper zooKeeper;

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public void createRpcServerNode(String data) throws Exception {
        //创建一个客户端程序, 对于注册可以不用监听事件
        zooKeeper = new ZooKeeper(registryAddress, Constant.SESSION_TIMEOUT, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
            }
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
                log.error("Failed to create node", e.getMessage());
            }
        } else {
            log.error("ZooKeeper connect is null");
        }
    }
}
