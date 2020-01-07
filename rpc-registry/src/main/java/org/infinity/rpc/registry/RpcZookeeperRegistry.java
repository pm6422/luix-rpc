package org.infinity.rpc.registry;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RpcZookeeperRegistry {

    public static final Logger    LOGGER = LoggerFactory.getLogger(RpcZookeeperRegistry.class);
    private             String    registryAddress;
    private             ZooKeeper zooKeeper;

    public String getRegistryAddress() {
        return registryAddress;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public void createNode(String data) throws Exception {
        //创建一个客户端程序, 对于注册可以不用监听事件
        zooKeeper = new ZooKeeper(registryAddress, Constant.SESSION_TIMEOUT, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
            }
        });
        if (zooKeeper != null) {
            try {
                //判断注册的目录是否存在
                Stat stat = zooKeeper.exists(Constant.REGISTRY_PATH, false);
                if (stat == null) {
                    //如果不存在, 创建一个持久的节点目录
                    zooKeeper.create(Constant.REGISTRY_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
                LOGGER.debug(this.getData().toString());
                //创建一个临时的序列节点,并且保存数据信息
                zooKeeper.create(Constant.DATA_PATH, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
                LOGGER.debug(this.getData().toString());
                LOGGER.debug("Created a temporary node");
            } catch (Exception e) {
                LOGGER.error("", e);
                e.printStackTrace();
            }
        } else {
            LOGGER.debug("zooKeeper connect is null");
        }
    }

    private List<String> getData() {
        List<String> dataList = new ArrayList<>();
        try {
            //获取子节点信息
            List<String> nodeList = zooKeeper.getChildren(Constant.REGISTRY_PATH, true);
            for (String node : nodeList) {
                byte[] bytes = zooKeeper.getData(Constant.REGISTRY_PATH + "/" + node, false, null);
                dataList.add(new String(bytes));
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return dataList;
    }

    //测试程序
    public static void main(String[] args) throws Exception {
        RpcZookeeperRegistry rpcZookeeperRegistry = new RpcZookeeperRegistry();
        rpcZookeeperRegistry.setRegistryAddress("127.0.0.1:2181");
        rpcZookeeperRegistry.createNode("testdata");
        //让程序等待输入,程序一直处于运行状态
        System.in.read();
    }
}
