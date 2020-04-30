package org.infinity.rpc.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.Watcher;
import org.infinity.rpc.core.destory.Closable;
import org.infinity.rpc.core.registry.CommandFailbackRegistry;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.registry.zookeeper.utils.ZkUtils;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Zookeeper registry implementation used to subscribe, unsubscribe, register or unregister data.
 */
@Slf4j
public class ZookeeperRegistry extends CommandFailbackRegistry implements Closable {
    private final Lock                                                           clientLock           = new ReentrantLock();
    private final Lock                                                           serverLock           = new ReentrantLock();
    private final Set<Url>                                                       availableServiceUrls = new ConcurrentHashSet<>();
    private final Map<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> serviceListeners     = new ConcurrentHashMap<>();
    private final Map<Url, ConcurrentHashMap<CommandListener, IZkDataListener>>  commandListeners     = new ConcurrentHashMap<>();
    private       ZkClient                                                       zkClient;

    public ZookeeperRegistry(Url url, ZkClient zkClient) {
        super(url);
        this.zkClient = zkClient;
        IZkStateListener zkStateListener = new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
                // do nothing intentionally
            }

            /**
             * Called after the zookeeper session has expired and a new session has been created.
             * You would have to re-create any ephemeral nodes here.
             * @throws Exception On any error
             */
            @Override
            public void handleNewSession() throws Exception {
                log.info("Received a new zookeeper session notification");
                reconnectService();
                reconnectClient();
            }

            @Override
            public void handleSessionEstablishmentError(Throwable error) throws Exception {
                // do nothing intentionally
            }
        };
        // Subscribe zk listener
        zkClient.subscribeStateChanges(zkStateListener);
    }

    /**
     * Re-register the providers
     */
    private void reconnectService() {
        Collection<Url> allRegisteredServices = super.getRegisteredServiceUrls();
        if (CollectionUtils.isEmpty(allRegisteredServices)) {
            return;
        }
        try {
            serverLock.lock();
            for (Url url : super.getRegisteredServiceUrls()) {
                // re-register after a new session
                doRegister(url);
            }
            log.info("Re-registered all the providers after a reconnection to zookeeper");

            for (Url availableServiceUrl : availableServiceUrls) {
                if (!super.getRegisteredServiceUrls().contains(availableServiceUrl)) {
                    log.warn("Url [{}] has not been registered!", availableServiceUrl);
                    continue;
                }
                doActivate(availableServiceUrl);
            }
            log.info("[{}] reconnect: available services {}", registryClassName, availableServiceUrls);
        } finally {
            serverLock.unlock();
        }
    }

    /**
     *
     */
    private void reconnectClient() {
        if (MapUtils.isEmpty(serviceListeners)) {
            return;
        }
        try {
            clientLock.lock();
            for (Map.Entry entry : serviceListeners.entrySet()) {
                Url url = (Url) entry.getKey();
                ConcurrentHashMap<ServiceListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
                if (childChangeListeners != null) {
                    for (Map.Entry e : childChangeListeners.entrySet()) {
                        subscribeService(url, (ServiceListener) e.getKey());
                    }
                }
            }
            for (Map.Entry entry : commandListeners.entrySet()) {
                Url url = (Url) entry.getKey();
                ConcurrentHashMap<CommandListener, IZkDataListener> dataChangeListeners = commandListeners.get(url);
                if (dataChangeListeners != null) {
                    for (Map.Entry e : dataChangeListeners.entrySet()) {
                        subscribeCommand(url, (CommandListener) e.getKey());
                    }
                }
            }
            log.info("[{}] reconnect all clients", registryClassName);
        } finally {
            clientLock.unlock();
        }
    }

    /**
     * Register specified url info to zookeeper inactive node
     *
     * @param url
     */
    @Override
    protected void doActivate(Url url) {
        try {
            serverLock.lock();
            if (url == null) {
                availableServiceUrls.addAll(super.getRegisteredServiceUrls());
                for (Url u : super.getRegisteredServiceUrls()) {
                    // Remove the dirty data node
                    removeNode(u, ZkNodeType.ACTIVE_SERVER);
                    removeNode(u, ZkNodeType.INACTIVE_SERVER);
                    // Create data under available server node
                    createNode(u, ZkNodeType.ACTIVE_SERVER);
                }
            } else {
                availableServiceUrls.add(url);
                // Remove the dirty data node
                removeNode(url, ZkNodeType.ACTIVE_SERVER);
                removeNode(url, ZkNodeType.INACTIVE_SERVER);
                // Create data under available server node
                createNode(url, ZkNodeType.ACTIVE_SERVER);
            }
        } finally {
            serverLock.unlock();
        }
    }

    @Override
    protected void doDeactivate(Url url) {
        try {
            serverLock.lock();
            if (url == null) {
                availableServiceUrls.removeAll(getRegisteredServiceUrls());
                for (Url u : getRegisteredServiceUrls()) {
                    // Remove the dirty data node
                    removeNode(u, ZkNodeType.ACTIVE_SERVER);
                    removeNode(u, ZkNodeType.INACTIVE_SERVER);
                    // Create data under available server node
                    createNode(u, ZkNodeType.INACTIVE_SERVER);
                }
            } else {
                availableServiceUrls.remove(url);
                // Remove the dirty data node
                removeNode(url, ZkNodeType.ACTIVE_SERVER);
                removeNode(url, ZkNodeType.INACTIVE_SERVER);
                // Create data under available server node
                createNode(url, ZkNodeType.INACTIVE_SERVER);
            }
        } finally {
            serverLock.unlock();
        }
    }

    /**
     * Register specified url info to zookeeper
     *
     * @param url
     */
    @Override
    protected void doRegister(Url url) {
        try {
            serverLock.lock();
            // Remove old node in order to avoid using dirty data
            removeNode(url, ZkNodeType.ACTIVE_SERVER);
            removeNode(url, ZkNodeType.INACTIVE_SERVER);
            // Create data under unavailable server node
            createNode(url, ZkNodeType.INACTIVE_SERVER);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to register [{0}] to zookeeper [{1}] with the error: {2}", url, getRegistryUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    /**
     * Delete specified directory
     *
     * @param url      url
     * @param nodeType directory
     */
    private void removeNode(Url url, ZkNodeType nodeType) {
        String nodePath = ZkUtils.toNodePath(url, nodeType);
        if (zkClient.exists(nodePath)) {
            zkClient.delete(nodePath);
        }
    }

    /**
     * Create zookeeper persistent and ephemeral node
     *
     * @param url      url
     * @param nodeType directory
     */
    private void createNode(Url url, ZkNodeType nodeType) {
        String nodeTypePath = ZkUtils.toNodeTypePath(url, nodeType);
        if (!zkClient.exists(nodeTypePath)) {
            // Create a persistent directory
            zkClient.createPersistent(nodeTypePath, true);
        }
        // Create a temporary node
        zkClient.createEphemeral(ZkUtils.toNodePath(url, nodeType), url.toFullStr());
    }

    @Override
    protected void doUnregister(Url url) {
        try {
            serverLock.lock();
            removeNode(url, ZkNodeType.ACTIVE_SERVER);
            removeNode(url, ZkNodeType.INACTIVE_SERVER);
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to unregister %s to zookeeper(%s), cause: %s", url, getRegistryUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    @Override
    protected void subscribeService(Url url, final ServiceListener serviceListener) {
        try {
            clientLock.lock();
            ConcurrentHashMap<ServiceListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
            if (childChangeListeners == null) {
                serviceListeners.putIfAbsent(url, new ConcurrentHashMap<>());
                childChangeListeners = serviceListeners.get(url);
            }
            IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
            if (zkChildListener == null) {
                childChangeListeners.putIfAbsent(serviceListener, (parentPath, currentChilds) -> {
                    serviceListener.notifyService(url, getRegistryUrl(), nodeChildsToUrls(url, parentPath, currentChilds));
                    log.info(String.format("[ZookeeperRegistry] service list change: path=%s, currentChilds=%s", parentPath, currentChilds.toString()));
                });
                zkChildListener = childChangeListeners.get(serviceListener);
            }

            try {
                // 防止旧节点未正常注销
                removeNode(url, ZkNodeType.CLIENT);
                createNode(url, ZkNodeType.CLIENT);
            } catch (Exception e) {
                log.warn("[ZookeeperRegistry] subscribe service: create node error, path=%s, msg=%s", ZkUtils.toNodePath(url, ZkNodeType.CLIENT), e.getMessage());
            }

            String serverTypePath = ZkUtils.toNodeTypePath(url, ZkNodeType.ACTIVE_SERVER);
            zkClient.subscribeChildChanges(serverTypePath, zkChildListener);
            log.info(String.format("[ZookeeperRegistry] subscribe service: path=%s, info=%s", ZkUtils.toNodePath(url, ZkNodeType.ACTIVE_SERVER), url.toFullStr()));
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to subscribe %s to zookeeper(%s), cause: %s", url, getRegistryUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected void subscribeCommand(Url url, final CommandListener commandListener) {
        try {
            clientLock.lock();
            ConcurrentHashMap<CommandListener, IZkDataListener> dataChangeListeners = commandListeners.get(url);
            if (dataChangeListeners == null) {
                commandListeners.putIfAbsent(url, new ConcurrentHashMap<>());
                dataChangeListeners = commandListeners.get(url);
            }
            IZkDataListener zkDataListener = dataChangeListeners.get(commandListener);
            if (zkDataListener == null) {
                dataChangeListeners.putIfAbsent(commandListener, new IZkDataListener() {
                    @Override
                    public void handleDataChange(String dataPath, Object data) throws Exception {
                        commandListener.notifyCommand(url, (String) data);
                        log.info(String.format("[ZookeeperRegistry] command data change: path=%s, command=%s", dataPath, (String) data));
                    }

                    @Override
                    public void handleDataDeleted(String dataPath) throws Exception {
                        commandListener.notifyCommand(url, null);
                        log.info(String.format("[ZookeeperRegistry] command deleted: path=%s", dataPath));
                    }
                });
                zkDataListener = dataChangeListeners.get(commandListener);
            }

            String commandPath = ZkUtils.toCommandPath(url);
            zkClient.subscribeDataChanges(commandPath, zkDataListener);
            log.info(String.format("[ZookeeperRegistry] subscribe command: path=%s, info=%s", commandPath, url.toFullStr()));
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to subscribe %s to zookeeper(%s), cause: %s", url, getRegistryUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected void unsubscribeService(Url url, ServiceListener serviceListener) {
        try {
            clientLock.lock();
            Map<ServiceListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
            if (childChangeListeners != null) {
                IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
                if (zkChildListener != null) {
                    zkClient.unsubscribeChildChanges(ZkUtils.toNodeTypePath(url, ZkNodeType.CLIENT), zkChildListener);
                    childChangeListeners.remove(serviceListener);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to unsubscribe service %s to zookeeper(%s), cause: %s", url, getRegistryUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected void unsubscribeCommand(Url url, CommandListener commandListener) {
        try {
            clientLock.lock();
            Map<CommandListener, IZkDataListener> dataChangeListeners = commandListeners.get(url);
            if (dataChangeListeners != null) {
                IZkDataListener zkDataListener = dataChangeListeners.get(commandListener);
                if (zkDataListener != null) {
                    zkClient.unsubscribeDataChanges(ZkUtils.toCommandPath(url), zkDataListener);
                    dataChangeListeners.remove(commandListener);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to unsubscribe command %s to zookeeper(%s), cause: %s", url, getRegistryUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected List<Url> discoverService(Url url) {
        try {
            String parentPath = ZkUtils.toNodeTypePath(url, ZkNodeType.ACTIVE_SERVER);
            List<String> currentChilds = new ArrayList<>();
            if (zkClient.exists(parentPath)) {
                currentChilds = zkClient.getChildren(parentPath);
            }
            return nodeChildsToUrls(url, parentPath, currentChilds);
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to discover service %s from zookeeper(%s), cause: %s", url, getRegistryUrl(), e.getMessage()), e);
        }
    }

    @Override
    protected String discoverCommand(Url url) {
        try {
            String commandPath = ZkUtils.toCommandPath(url);
            String command = "";
            if (zkClient.exists(commandPath)) {
                command = zkClient.readData(commandPath);
            }
            return command;
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to discover command %s from zookeeper(%s), cause: %s", url, getRegistryUrl(), e.getMessage()));
        }
    }

    private List<Url> nodeChildsToUrls(Url url, String parentPath, List<String> currentChilds) {
        List<Url> urls = new ArrayList<>();
        if (currentChilds != null) {
            for (String node : currentChilds) {
                String nodePath = parentPath + Url.PATH_SEPARATOR + node;
                String data = null;
                try {
                    data = zkClient.readData(nodePath, true);
                } catch (Exception e) {
                    log.warn("get zkdata fail!" + e.getMessage());
                }
                Url newurl = null;
                if (StringUtils.isNotBlank(data)) {
                    try {
                        newurl = Url.valueOf(data);
                    } catch (Exception e) {
                        log.warn(String.format("Found malformed urls from ZookeeperRegistry, path=%s", nodePath), e);
                    }
                }
                if (newurl == null) {
                    newurl = url.copy();
                    String host = "";
                    int port = 80;
                    if (node.indexOf(":") > -1) {
                        String[] hp = node.split(":");
                        if (hp.length > 1) {
                            host = hp[0];
                            try {
                                port = Integer.parseInt(hp[1]);
                            } catch (Exception ignore) {
                            }
                        }
                    } else {
                        host = node;
                    }
                    newurl.setHost(host);
                    newurl.setPort(port);
                }
                urls.add(newurl);
            }
        }
        return urls;
    }

    @Override
    public void close() {
        this.zkClient.close();
    }

    public Map<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> getServiceListeners() {
        return serviceListeners;
    }

    public Map<Url, ConcurrentHashMap<CommandListener, IZkDataListener>> getCommandListeners() {
        return commandListeners;
    }
}
