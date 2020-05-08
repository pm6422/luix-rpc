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
import org.infinity.rpc.core.registry.CommandFailbackAbstractRegistry;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;
import org.infinity.rpc.utilities.destory.Cleanable;

import javax.annotation.concurrent.ThreadSafe;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Zookeeper registry implementation used to subscribe, unsubscribe, register or unregister data.
 */
@Slf4j
@ThreadSafe
public class ZookeeperRegistry extends CommandFailbackAbstractRegistry implements Cleanable {
    private final Lock                                                           clientLock           = new ReentrantLock();
    private final Lock                                                           serverLock           = new ReentrantLock();
    private final Set<Url>                                                       availableServiceUrls = new ConcurrentHashSet<>();
    private final Map<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> providerListeners    = new ConcurrentHashMap<>();
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
                reregisterProviders();
                reregisterListeners();
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
     * Re-register the providers after a new zookeeper session
     * e.g, Zookeeper was shutdown after the infinity application startup, then zookeeper startup again will cause a new session.
     */
    private void reregisterProviders() {
        Collection<Url> allRegisteredServices = super.getRegisteredProviderUrls();
        if (CollectionUtils.isEmpty(allRegisteredServices)) {
            return;
        }
        try {
            serverLock.lock();
            for (Url url : super.getRegisteredProviderUrls()) {
                // re-register after a new session
                doRegister(url);
            }
            log.info("Re-registered all the providers after a reconnection to zookeeper");

            for (Url availableServiceUrl : availableServiceUrls) {
                if (!super.getRegisteredProviderUrls().contains(availableServiceUrl)) {
                    log.warn("Url [{}] has not been registered!", availableServiceUrl);
                    continue;
                }
                doActivate(availableServiceUrl);
            }
            log.info("Re-registered the provider urls after a new zookeeper session");
        } finally {
            serverLock.unlock();
        }
    }

    /**
     * Re-register the provider and command listeners after a new zookeeper session
     * e.g, Zookeeper was shutdown after the infinity application startup, then zookeeper startup again will cause a new session.
     */
    private void reregisterListeners() {
        try {
            clientLock.lock();
            if (MapUtils.isNotEmpty(providerListeners)) {
                for (Map.Entry<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> entry : providerListeners.entrySet()) {
                    Url url = entry.getKey();
                    ConcurrentHashMap<ServiceListener, IZkChildListener> childChangeListeners = providerListeners.get(url);
                    if (MapUtils.isNotEmpty(childChangeListeners)) {
                        for (Map.Entry<ServiceListener, IZkChildListener> e : childChangeListeners.entrySet()) {
                            subscribeServiceListener(url, e.getKey());
                        }
                    }
                }
                log.info("Re-registered the provider listeners after a new zookeeper session");
            }
            if (MapUtils.isNotEmpty(commandListeners)) {
                for (Map.Entry<Url, ConcurrentHashMap<CommandListener, IZkDataListener>> entry : commandListeners.entrySet()) {
                    Url url = entry.getKey();
                    ConcurrentHashMap<CommandListener, IZkDataListener> dataChangeListeners = commandListeners.get(url);
                    if (MapUtils.isNotEmpty(dataChangeListeners)) {
                        for (Map.Entry<CommandListener, IZkDataListener> e : dataChangeListeners.entrySet()) {
                            subscribeCommandListener(url, e.getKey());
                        }
                    }
                }
                log.info("Re-registered the command listeners after a new zookeeper session");
            }
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
                availableServiceUrls.addAll(super.getRegisteredProviderUrls());
                for (Url u : super.getRegisteredProviderUrls()) {
                    // Remove the dirty data node
                    removeNode(u, ZookeeperActiveStatusNode.ACTIVE_SERVER);
                    removeNode(u, ZookeeperActiveStatusNode.INACTIVE_SERVER);
                    // Create data under available server node
                    createNode(u, ZookeeperActiveStatusNode.ACTIVE_SERVER);
                }
            } else {
                availableServiceUrls.add(url);
                // Remove the dirty data node
                removeNode(url, ZookeeperActiveStatusNode.ACTIVE_SERVER);
                removeNode(url, ZookeeperActiveStatusNode.INACTIVE_SERVER);
                // Create data under available server node
                createNode(url, ZookeeperActiveStatusNode.ACTIVE_SERVER);
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
                availableServiceUrls.removeAll(getRegisteredProviderUrls());
                for (Url u : getRegisteredProviderUrls()) {
                    // Remove the dirty data node
                    removeNode(u, ZookeeperActiveStatusNode.ACTIVE_SERVER);
                    removeNode(u, ZookeeperActiveStatusNode.INACTIVE_SERVER);
                    // Create data under available server node
                    createNode(u, ZookeeperActiveStatusNode.INACTIVE_SERVER);
                }
            } else {
                availableServiceUrls.remove(url);
                // Remove the dirty data node
                removeNode(url, ZookeeperActiveStatusNode.ACTIVE_SERVER);
                removeNode(url, ZookeeperActiveStatusNode.INACTIVE_SERVER);
                // Create data under available server node
                createNode(url, ZookeeperActiveStatusNode.INACTIVE_SERVER);
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
            removeNode(url, ZookeeperActiveStatusNode.ACTIVE_SERVER);
            removeNode(url, ZookeeperActiveStatusNode.INACTIVE_SERVER);
            // Create data under unavailable server node
            createNode(url, ZookeeperActiveStatusNode.INACTIVE_SERVER);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to register [{0}] to zookeeper [{1}] with the error: {2}", url, getRegistryUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    /**
     * Delete specified directory
     *
     * @param url              url
     * @param activeStatusNode directory
     */
    private void removeNode(Url url, ZookeeperActiveStatusNode activeStatusNode) {
        String nodePath = ZookeeperUtils.getAddressPath(url, activeStatusNode);
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
    private void createNode(Url url, ZookeeperActiveStatusNode nodeType) {
        String nodeTypePath = ZookeeperUtils.getActiveNodePath(url, nodeType);
        if (!zkClient.exists(nodeTypePath)) {
            // Create a persistent directory
            zkClient.createPersistent(nodeTypePath, true);
        }
        // Create a temporary node
        zkClient.createEphemeral(ZookeeperUtils.getAddressPath(url, nodeType), url.toFullStr());
    }

    /**
     * Unregister specified url info from zookeeper
     *
     * @param url url
     */
    @Override
    protected void doUnregister(Url url) {
        try {
            serverLock.lock();
            removeNode(url, ZookeeperActiveStatusNode.ACTIVE_SERVER);
            removeNode(url, ZookeeperActiveStatusNode.INACTIVE_SERVER);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to unregister [{0}] from zookeeper [{1}] with the error: {2}", url, getRegistryUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    /**
     * Discover providers url for single node or cluster environment
     *
     * @param url url
     * @return provider urls
     */
    @Override
    protected List<Url> discoverProviders(Url url) {
        try {
            String parentPath = ZookeeperUtils.getActiveNodePath(url, ZookeeperActiveStatusNode.ACTIVE_SERVER);
            List<String> addresses = new ArrayList<>();
            if (zkClient.exists(parentPath)) {
                addresses = zkClient.getChildren(parentPath);
            }
            return readUrl(addresses, parentPath, url);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to discover service [{0}] from zookeeper [{1}] with the error: {2}", url, getRegistryUrl(), e.getMessage()), e);
        }
    }

    /**
     * Read address file content as provider url
     *
     * @param addresses addresses
     * @param path      zookeeper path
     * @param url       url
     * @return provider urls
     */
    private List<Url> readUrl(List<String> addresses, String path, Url url) {
        List<Url> urls = new ArrayList<>();
        if (CollectionUtils.isEmpty(addresses)) {
            return urls;
        }
        for (String address : addresses) {
            String addrFilePath = path.concat(Url.PATH_SEPARATOR).concat(address);
            String addrFileData = null;
            try {
                addrFileData = zkClient.readData(addrFilePath, true);
            } catch (Exception e) {
                log.warn("Failed to read the zookeeper file data!");
            }
            Url newurl = null;
            if (StringUtils.isNotBlank(addrFileData)) {
                try {
                    newurl = Url.valueOf(addrFileData);
                } catch (Exception e) {
                    log.warn(MessageFormat.format("Found illegal zookeeper file data with path [{0}]", addrFilePath), e);
                }
            }
            // TODO: remove the useless code snippet
//            if (newurl == null) {
//                newurl = url.copy();
//                String host = "";
//                int port = 80;
//                if (address.indexOf(":") > -1) {
//                    String[] hp = address.split(":");
//                    if (hp.length > 1) {
//                        host = hp[0];
//                        try {
//                            port = Integer.parseInt(hp[1]);
//                        } catch (Exception ignore) {
//                        }
//                    }
//                } else {
//                    host = address;
//                }
//                newurl.setHost(host);
//                newurl.setPort(port);
//            }
            urls.add(newurl);
        }
        return urls;
    }

    /**
     * Read command json content of specified url
     *
     * @param url url
     * @return command json string
     */
    @Override
    protected String discoverCommand(Url url) {
        try {
            String commandPath = ZookeeperUtils.getCommandPath(url);
            String command = "";
            if (zkClient.exists(commandPath)) {
                command = zkClient.readData(commandPath);
            }
            return command;
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to discover command [{0}] from zookeeper [{1}]", url, getRegistryUrl()), e);
        }
    }

    /**
     * @param url
     * @param serviceListener
     */
    @Override
    protected void subscribeServiceListener(Url url, ServiceListener serviceListener) {
        try {
            clientLock.lock();
            Map<ServiceListener, IZkChildListener> childChangeListeners = providerListeners.get(url);
            if (childChangeListeners == null) {
                providerListeners.putIfAbsent(url, new ConcurrentHashMap<>());
                childChangeListeners = providerListeners.get(url);
            }
            IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
            if (zkChildListener == null) {
                childChangeListeners.putIfAbsent(serviceListener, (parentPath, currentChilds) -> {
                    // trigger user customized service listener
                    serviceListener.onSubscribe(url, getRegistryUrl(), readUrl(currentChilds, parentPath, url));
                    log.info("Provider addresses list changed with current value {} under path [{}]", currentChilds.toString(), parentPath);
                });
                zkChildListener = childChangeListeners.get(serviceListener);
            }

            try {
                // Remove dirty data
                removeNode(url, ZookeeperActiveStatusNode.CLIENT);
                createNode(url, ZookeeperActiveStatusNode.CLIENT);
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to remove or create the node with path [{0}]", ZookeeperUtils.getAddressPath(url, ZookeeperActiveStatusNode.CLIENT)), e);
            }

            String path = ZookeeperUtils.getActiveNodePath(url, ZookeeperActiveStatusNode.ACTIVE_SERVER);
            // Bind the path with zookeeper child change listener, any the child list changes under the path will trigger the zkChildListener
            zkClient.subscribeChildChanges(path, zkChildListener);
            log.info("Subscribed the listener for the path [{}]", ZookeeperUtils.getAddressPath(url, ZookeeperActiveStatusNode.ACTIVE_SERVER));
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to subscribe listeners for url [{}]", url), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected void unsubscribeServiceListener(Url url, ServiceListener serviceListener) {
        try {
            clientLock.lock();
            Map<ServiceListener, IZkChildListener> childChangeListeners = providerListeners.get(url);
            if (childChangeListeners == null) {
                return;
            }
            IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
            if (zkChildListener != null) {
                // Unbind the path with zookeeper child change listener
                zkClient.unsubscribeChildChanges(ZookeeperUtils.getActiveNodePath(url, ZookeeperActiveStatusNode.CLIENT), zkChildListener);
                childChangeListeners.remove(serviceListener);
            }
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to unsubscribe listeners for url [{}]", url), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected void subscribeCommandListener(Url url, final CommandListener commandListener) {
        try {
            clientLock.lock();
            Map<CommandListener, IZkDataListener> dataChangeListeners = commandListeners.get(url);
            if (dataChangeListeners == null) {
                commandListeners.putIfAbsent(url, new ConcurrentHashMap<>());
                dataChangeListeners = commandListeners.get(url);
            }
            IZkDataListener zkDataListener = dataChangeListeners.get(commandListener);
            if (zkDataListener == null) {
                dataChangeListeners.putIfAbsent(commandListener, new IZkDataListener() {
                    @Override
                    public void handleDataChange(String dataPath, Object data) {
                        commandListener.onSubscribe(url, (String) data);
                        log.info("Command data changed with current value {} under path [{}]", data.toString(), dataPath);
                    }

                    @Override
                    public void handleDataDeleted(String dataPath) {
                        commandListener.onSubscribe(url, null);
                        log.info("Command data deleted under path [{}]", dataPath);
                    }
                });
                zkDataListener = dataChangeListeners.get(commandListener);
            }

            String commandPath = ZookeeperUtils.getCommandPath(url);
            // Bind the path with zookeeper data change listener, any the data changes under the path will trigger the zkDataListener
            zkClient.subscribeDataChanges(commandPath, zkDataListener);
            log.info(String.format("[ZookeeperRegistry] subscribe command: path=%s, info=%s", commandPath, url.toFullStr()));
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to subscribe %s to zookeeper(%s)", url, getRegistryUrl()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected void unsubscribeCommandListener(Url url, CommandListener commandListener) {
        try {
            clientLock.lock();
            Map<CommandListener, IZkDataListener> dataChangeListeners = commandListeners.get(url);
            if (dataChangeListeners != null) {
                IZkDataListener zkDataListener = dataChangeListeners.get(commandListener);
                if (zkDataListener != null) {
                    zkClient.unsubscribeDataChanges(ZookeeperUtils.getCommandPath(url), zkDataListener);
                    dataChangeListeners.remove(commandListener);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to unsubscribe command %s to zookeeper(%s)", url, getRegistryUrl()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    public void cleanup() {
        this.zkClient.close();
    }

    public Map<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> getProviderListeners() {
        return providerListeners;
    }

    public Map<Url, ConcurrentHashMap<CommandListener, IZkDataListener>> getCommandListeners() {
        return commandListeners;
    }
}
