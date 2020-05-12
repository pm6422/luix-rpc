package org.infinity.rpc.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.Watcher;
import org.infinity.rpc.core.registry.CommandFailbackAbstractRegistry;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.infinity.rpc.utilities.annotation.Event;
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
    private final Lock                                                           clientLock                       = new ReentrantLock();
    private final Lock                                                           serverLock                       = new ReentrantLock();
    private final Set<Url>                                                       availableServiceUrls             = new ConcurrentHashSet<>();
    private final Map<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> providerListenersPerClientUrlMap = new ConcurrentHashMap<>();
    private final Map<Url, ConcurrentHashMap<CommandListener, IZkDataListener>>  commandListenersPerClientUrlMap  = new ConcurrentHashMap<>();
    private       ZkClient                                                       zkClient;

    @Event
    public ZookeeperRegistry(Url registryUrl, ZkClient zkClient) {
        super(registryUrl);
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
     * Default access modifier for unit test
     *
     * @return provider listeners
     */
    Map<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> getProviderListenersPerClientUrlMap() {
        return providerListenersPerClientUrlMap;
    }

    /**
     * Default access modifier for unit test
     *
     * @return command listeners
     */
    Map<Url, ConcurrentHashMap<CommandListener, IZkDataListener>> getCommandListenersPerClientUrlMap() {
        return commandListenersPerClientUrlMap;
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
            if (MapUtils.isNotEmpty(providerListenersPerClientUrlMap)) {
                for (Map.Entry<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> entry : providerListenersPerClientUrlMap.entrySet()) {
                    Url url = entry.getKey();
                    ConcurrentHashMap<ServiceListener, IZkChildListener> childChangeListeners = providerListenersPerClientUrlMap.get(url);
                    if (MapUtils.isNotEmpty(childChangeListeners)) {
                        for (Map.Entry<ServiceListener, IZkChildListener> e : childChangeListeners.entrySet()) {
                            subscribeServiceListener(url, e.getKey());
                        }
                    }
                }
                log.info("Re-registered the provider listeners after a new zookeeper session");
            }
            if (MapUtils.isNotEmpty(commandListenersPerClientUrlMap)) {
                for (Map.Entry<Url, ConcurrentHashMap<CommandListener, IZkDataListener>> entry : commandListenersPerClientUrlMap.entrySet()) {
                    Url url = entry.getKey();
                    ConcurrentHashMap<CommandListener, IZkDataListener> dataChangeListeners = commandListenersPerClientUrlMap.get(url);
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
     * Register specified url info to zookeeper active node
     *
     * @param providerUrl provider url
     */
    @Override
    protected void doActivate(Url providerUrl) {
        try {
            serverLock.lock();
            if (providerUrl == null) {
                // Register all provider urls to 'active' node if parameter url is null
                availableServiceUrls.addAll(super.getRegisteredProviderUrls());
                for (Url u : super.getRegisteredProviderUrls()) {
                    // Remove the dirty data node
                    removeNode(u, ZookeeperActiveStatusNode.ACTIVE_SERVER);
                    removeNode(u, ZookeeperActiveStatusNode.INACTIVE_SERVER);
                    // Create data under 'active' node
                    createNode(u, ZookeeperActiveStatusNode.ACTIVE_SERVER);
                }
            } else {
                availableServiceUrls.add(providerUrl);
                // Remove the dirty data node
                removeNode(providerUrl, ZookeeperActiveStatusNode.ACTIVE_SERVER);
                removeNode(providerUrl, ZookeeperActiveStatusNode.INACTIVE_SERVER);
                // Create data under 'active' node
                createNode(providerUrl, ZookeeperActiveStatusNode.ACTIVE_SERVER);
            }
        } finally {
            serverLock.unlock();
        }
    }

    /**
     * Register specified url info to zookeeper inactive node
     *
     * @param providerUrl url
     */
    @Override
    protected void doDeactivate(Url providerUrl) {
        try {
            serverLock.lock();
            if (providerUrl == null) {
                // Register all provider urls to 'inactive' node if parameter url is null
                availableServiceUrls.removeAll(getRegisteredProviderUrls());
                for (Url u : getRegisteredProviderUrls()) {
                    // Remove the dirty data node
                    removeNode(u, ZookeeperActiveStatusNode.ACTIVE_SERVER);
                    removeNode(u, ZookeeperActiveStatusNode.INACTIVE_SERVER);
                    // Create data under 'inactive' node
                    createNode(u, ZookeeperActiveStatusNode.INACTIVE_SERVER);
                }
            } else {
                availableServiceUrls.remove(providerUrl);
                // Remove the dirty data node
                removeNode(providerUrl, ZookeeperActiveStatusNode.ACTIVE_SERVER);
                removeNode(providerUrl, ZookeeperActiveStatusNode.INACTIVE_SERVER);
                // Create data under 'inactive' node
                createNode(providerUrl, ZookeeperActiveStatusNode.INACTIVE_SERVER);
            }
        } finally {
            serverLock.unlock();
        }
    }

    /**
     * Register specified url info to zookeeper
     *
     * @param providerUrl provider url
     */
    @Override
    protected void doRegister(Url providerUrl) {
        try {
            serverLock.lock();
            // Remove old node in order to avoid using dirty data
            removeNode(providerUrl, ZookeeperActiveStatusNode.ACTIVE_SERVER);
            removeNode(providerUrl, ZookeeperActiveStatusNode.INACTIVE_SERVER);
            // Create data under unavailable server node
            createNode(providerUrl, ZookeeperActiveStatusNode.INACTIVE_SERVER);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to register [{0}] to zookeeper [{1}] with the error: {2}", providerUrl, getRegistryUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    /**
     * Create zookeeper persistent directory and ephemeral root node
     *
     * @param providerUrl url
     * @param nodeType    directory
     */
    private void createNode(Url providerUrl, ZookeeperActiveStatusNode nodeType) {
        String nodeTypePath = ZookeeperUtils.getActiveNodePath(providerUrl, nodeType);
        if (!zkClient.exists(nodeTypePath)) {
            // Create a persistent directory
            zkClient.createPersistent(nodeTypePath, true);
        }
        // Create a temporary file, which name is an address(host:port), which content is the full string of the url
        zkClient.createEphemeral(ZookeeperUtils.getAddressPath(providerUrl, nodeType), providerUrl.toFullStr());
    }

    /**
     * Delete specified directory
     *
     * @param providerUrl      provider url
     * @param activeStatusNode directory
     */
    private void removeNode(Url providerUrl, ZookeeperActiveStatusNode activeStatusNode) {
        String nodePath = ZookeeperUtils.getAddressPath(providerUrl, activeStatusNode);
        if (zkClient.exists(nodePath)) {
            zkClient.delete(nodePath);
        }
    }

    /**
     * Unregister specified url info from zookeeper
     *
     * @param providerUrl provider url
     */
    @Override
    protected void doUnregister(Url providerUrl) {
        try {
            serverLock.lock();
            removeNode(providerUrl, ZookeeperActiveStatusNode.ACTIVE_SERVER);
            removeNode(providerUrl, ZookeeperActiveStatusNode.INACTIVE_SERVER);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to unregister [{0}] from zookeeper [{1}] with the error: {2}", providerUrl, getRegistryUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    /**
     * Discover active providers' address file name list under single node or cluster environment
     *
     * @param clientUrl client url
     * @return provider urls
     */
    @Override
    protected List<Url> discoverActiveProviders(Url clientUrl) {
        try {
            String parentPath = ZookeeperUtils.getActiveNodePath(clientUrl, ZookeeperActiveStatusNode.ACTIVE_SERVER);
            List<String> addrFiles = new ArrayList<>();
            if (zkClient.exists(parentPath)) {
                addrFiles = zkClient.getChildren(parentPath);
            }
            return readProviderUrls(addrFiles, parentPath, clientUrl);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to discover service [{0}] from zookeeper [{1}] with the error: {2}", clientUrl, getRegistryUrl(), e.getMessage()), e);
        }
    }

    /**
     * Read provider urls from address files' data
     *
     * @param addrFiles address file list
     * @param path      zookeeper path
     * @param url       url
     * @return provider urls
     */
    private List<Url> readProviderUrls(List<String> addrFiles, String path, Url url) {
        List<Url> urls = new ArrayList<>();
        if (CollectionUtils.isEmpty(addrFiles)) {
            return urls;
        }
        for (String addrFile : addrFiles) {
            String addrFilePath = path.concat(Url.PATH_SEPARATOR).concat(addrFile);
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
//                if (addrFile.indexOf(":") > -1) {
//                    String[] hp = addrFile.split(":");
//                    if (hp.length > 1) {
//                        host = hp[0];
//                        try {
//                            port = Integer.parseInt(hp[1]);
//                        } catch (Exception ignore) {
//                        }
//                    }
//                } else {
//                    host = addrFile;
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
     * @param clientUrl client url
     * @return command json string
     */
    @Override
    protected String readCommand(Url clientUrl) {
        try {
            String commandPath = ZookeeperUtils.getCommandPath(clientUrl);
            String command = "";
            if (zkClient.exists(commandPath)) {
                command = zkClient.readData(commandPath);
            }
            return command;
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to discover command [{0}] from zookeeper [{1}]", clientUrl, getRegistryUrl()), e);
        }
    }

    /**
     * Monitor the specified zookeeper node linked to url whether the child nodes have been changed, and it will invoke custom service listener if child nodes change.
     *
     * @param clientUrl       client url to identify the zookeeper path
     * @param serviceListener service listener
     */
    @Override
    @Event
    protected void subscribeServiceListener(Url clientUrl, ServiceListener serviceListener) {
        try {
            clientLock.lock();
            Map<ServiceListener, IZkChildListener> childChangeListeners = providerListenersPerClientUrlMap.get(clientUrl);
            if (childChangeListeners == null) {
                providerListenersPerClientUrlMap.putIfAbsent(clientUrl, new ConcurrentHashMap<>());
                childChangeListeners = providerListenersPerClientUrlMap.get(clientUrl);
            }
            IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
            if (zkChildListener == null) {
                // Trigger user customized listener if child changes
                zkChildListener = new IZkChildListener() {
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                        List<String> addrFiles = ListUtils.emptyIfNull(currentChilds);
                        serviceListener.onSubscribe(clientUrl, getRegistryUrl(), readProviderUrls(addrFiles, parentPath, clientUrl));
                        log.info("Provider address files changed with current value {} under path [{}]", addrFiles.toString(), parentPath);
                    }
                };
                childChangeListeners.putIfAbsent(serviceListener, zkChildListener);
            }

            try {
                // Remove dirty data
                removeNode(clientUrl, ZookeeperActiveStatusNode.CLIENT);
                createNode(clientUrl, ZookeeperActiveStatusNode.CLIENT);
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to remove or create the node with path [{0}]", ZookeeperUtils.getAddressPath(clientUrl, ZookeeperActiveStatusNode.CLIENT)), e);
            }

            String path = ZookeeperUtils.getActiveNodePath(clientUrl, ZookeeperActiveStatusNode.ACTIVE_SERVER);
            // Bind the path with zookeeper child change listener, any the child list changes under the path will trigger the zkChildListener
            zkClient.subscribeChildChanges(path, zkChildListener);
            log.info("Subscribed the listener for the path [{}]", ZookeeperUtils.getAddressPath(clientUrl, ZookeeperActiveStatusNode.ACTIVE_SERVER));
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to subscribe listeners for url [{}]", clientUrl), e);
        } finally {
            clientLock.unlock();
        }
    }

    /**
     * Unsubscribe the service listener from the specified zookeeper node
     *
     * @param clientUrl       client url to identify the zookeeper path
     * @param serviceListener service listener
     */
    @Override
    protected void unsubscribeServiceListener(Url clientUrl, ServiceListener serviceListener) {
        try {
            clientLock.lock();
            Map<ServiceListener, IZkChildListener> childChangeListeners = providerListenersPerClientUrlMap.get(clientUrl);
            if (childChangeListeners == null) {
                return;
            }
            IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
            if (zkChildListener != null) {
                // Unbind the path with zookeeper child change listener
                zkClient.unsubscribeChildChanges(ZookeeperUtils.getActiveNodePath(clientUrl, ZookeeperActiveStatusNode.CLIENT), zkChildListener);
                childChangeListeners.remove(serviceListener);
            }
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to unsubscribe listeners for url [{}]", clientUrl), e);
        } finally {
            clientLock.unlock();
        }
    }

    /**
     * Monitor the specified zookeeper node linked to url whether the data have been changed, and it will invoke custom command listener if data change.
     *
     * @param clientUrl       client url to identify the zookeeper path
     * @param commandListener command listener
     */
    @Override
    @Event
    protected void subscribeCommandListener(Url clientUrl, final CommandListener commandListener) {
        try {
            clientLock.lock();
            Map<CommandListener, IZkDataListener> dataChangeListeners = commandListenersPerClientUrlMap.get(clientUrl);
            if (dataChangeListeners == null) {
                commandListenersPerClientUrlMap.putIfAbsent(clientUrl, new ConcurrentHashMap<>());
                dataChangeListeners = commandListenersPerClientUrlMap.get(clientUrl);
            }
            IZkDataListener zkDataListener = dataChangeListeners.get(commandListener);
            if (zkDataListener == null) {
                // Trigger user customized listener if data changes
                zkDataListener = new IZkDataListener() {
                    @Override
                    public void handleDataChange(String dataPath, Object data) {
                        commandListener.onSubscribe(clientUrl, (String) data);
                        log.info("Command data changed with current value {} under path [{}]", data.toString(), dataPath);
                    }

                    @Override
                    public void handleDataDeleted(String dataPath) {
                        commandListener.onSubscribe(clientUrl, null);
                        log.info("Command data deleted under path [{}]", dataPath);
                    }
                };
                dataChangeListeners.putIfAbsent(commandListener, zkDataListener);
            }

            String commandPath = ZookeeperUtils.getCommandPath(clientUrl);
            // Bind the path with zookeeper data change listener, any the data changes under the path will trigger the zkDataListener
            zkClient.subscribeDataChanges(commandPath, zkDataListener);
            log.info(String.format("[ZookeeperRegistry] subscribe command: path=%s, info=%s", commandPath, clientUrl.toFullStr()));
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to subscribe %s to zookeeper(%s)", clientUrl, getRegistryUrl()), e);
        } finally {
            clientLock.unlock();
        }
    }

    /**
     * Unsubscribe the command listener from the specified zookeeper node
     *
     * @param clientUrl       client url to identify the zookeeper path
     * @param commandListener command listener
     */
    @Override
    protected void unsubscribeCommandListener(Url clientUrl, CommandListener commandListener) {
        try {
            clientLock.lock();
            Map<CommandListener, IZkDataListener> dataChangeListeners = commandListenersPerClientUrlMap.get(clientUrl);
            if (dataChangeListeners != null) {
                IZkDataListener zkDataListener = dataChangeListeners.get(commandListener);
                if (zkDataListener != null) {
                    zkClient.unsubscribeDataChanges(ZookeeperUtils.getCommandPath(clientUrl), zkDataListener);
                    dataChangeListeners.remove(commandListener);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Failed to unsubscribe command %s to zookeeper(%s)", clientUrl, getRegistryUrl()), e);
        } finally {
            clientLock.unlock();
        }
    }

    /**
     * Do cleanup stuff
     */
    @Override
    public void cleanup() {
        this.zkClient.close();
    }
}
