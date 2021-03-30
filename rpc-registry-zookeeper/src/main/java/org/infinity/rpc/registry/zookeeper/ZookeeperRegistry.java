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
import org.apache.commons.lang3.Validate;
import org.apache.zookeeper.Watcher;
import org.infinity.rpc.core.registry.AddressInfo;
import org.infinity.rpc.core.registry.CommandFailbackAbstractRegistry;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.infinity.rpc.utilities.annotation.EventPublisher;
import org.infinity.rpc.utilities.annotation.EventSubscriber;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;
import org.infinity.rpc.utilities.destory.Cleanable;

import javax.annotation.concurrent.ThreadSafe;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.infinity.rpc.core.constant.ProviderConstants.EXPOSED;
import static org.infinity.rpc.core.constant.RpcConstants.PATH_SEPARATOR;
import static org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils.PROVIDER_PATH;

/**
 * Zookeeper registry implementation used to subscribe, unsubscribe, register or unregister data.
 * Zookeeper has three vital listeners:
 * - IZkStateListener: It will be triggered when a new zk session created.
 * - IZkDataListener: It will be triggered when the file data has been changed.
 * - IZkChildListener: It will be triggered when the files under the node has been changed.
 */
@Slf4j
@ThreadSafe
public class ZookeeperRegistry extends CommandFailbackAbstractRegistry implements Cleanable {
    private final ZkClient                                                       zkClient;
    /**
     * Used to resolve concurrency problems for subscribe or unsubscribe service listeners
     */
    private final Lock                                                           listenerLock                  = new ReentrantLock();
    /**
     * Used to resolve concurrency problems for register or unregister providers
     */
    private final Lock                                                           providerLock                  = new ReentrantLock();
    private final Set<Url>                                                       activeProviderUrls            = new ConcurrentHashSet<>();
    private final Map<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> providerListenersPerClientUrl = new ConcurrentHashMap<>();
    private final Map<Url, ConcurrentHashMap<CommandListener, IZkDataListener>>  commandListenersPerClientUrl  = new ConcurrentHashMap<>();

    public ZookeeperRegistry(Url registryUrl, ZkClient zkClient) {
        super(registryUrl);
        Validate.notNull(zkClient, "Zookeeper client must NOT be null!");
        this.zkClient = zkClient;
        IZkStateListener zkStateListener = new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState state) {
                // do nothing intentionally
            }

            /**
             * Called after the zookeeper session expired or a new zookeeper session, e,g restart zookeeper
             * You would have to re-create any ephemeral nodes here.
             */
            @Override
            @EventPublisher({"providersChangeEvent", "providerDataChangeEvent"})
            public void handleNewSession() {
                log.info("Created a new zookeeper session");
                reregisterProviders();
                reregisterListeners();
            }

            @Override
            public void handleSessionEstablishmentError(Throwable error) {
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
    Map<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> getProviderListenersPerClientUrl() {
        return providerListenersPerClientUrl;
    }

    /**
     * Default access modifier for unit test
     *
     * @return command listeners
     */
    Map<Url, ConcurrentHashMap<CommandListener, IZkDataListener>> getCommandListenersPerClientUrl() {
        return commandListenersPerClientUrl;
    }

    /**
     * Re-register the providers after a new zookeeper session
     * e.g, Zookeeper was shutdown after the infinity application startup, then zookeeper startup again will cause a new session.
     */
    protected void reregisterProviders() {
        Set<Url> allRegisteredProviders = super.getRegisteredProviderUrls();
        if (CollectionUtils.isEmpty(allRegisteredProviders)) {
            return;
        }

        providerLock.lock();
        try {
            for (Url url : allRegisteredProviders) {
                // Re-register after a new session
                doRegister(url);
            }
            log.info("Re-registered all the providers after a reconnection to zookeeper");

            for (Url activeProviderUrl : activeProviderUrls) {
                // Only registered provider can be re-registered
                if (!allRegisteredProviders.contains(activeProviderUrl)) {
                    log.warn("Url [{}] has not been registered!", activeProviderUrl);
                    continue;
                }
                doActivate(activeProviderUrl);
            }
            log.info("Re-registered the provider urls after a new zookeeper session");
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * Re-register the provider and command listeners after a new zookeeper session
     * e.g, Zookeeper was shutdown after the infinity application startup, then zookeeper startup again will cause a new session.
     */
    protected void reregisterListeners() {
        try {
            listenerLock.lock();
            if (MapUtils.isNotEmpty(providerListenersPerClientUrl)) {
                for (Map.Entry<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> entry : providerListenersPerClientUrl.entrySet()) {
                    Url url = entry.getKey();
                    ConcurrentHashMap<ServiceListener, IZkChildListener> childChangeListeners = providerListenersPerClientUrl.get(url);
                    if (MapUtils.isNotEmpty(childChangeListeners)) {
                        for (Map.Entry<ServiceListener, IZkChildListener> e : childChangeListeners.entrySet()) {
                            subscribeServiceListener(url, e.getKey());
                        }
                    }
                }
                log.info("Re-registered the provider listeners after a new zookeeper session");
            }
            if (MapUtils.isNotEmpty(commandListenersPerClientUrl)) {
                for (Map.Entry<Url, ConcurrentHashMap<CommandListener, IZkDataListener>> entry : commandListenersPerClientUrl.entrySet()) {
                    Url url = entry.getKey();
                    ConcurrentHashMap<CommandListener, IZkDataListener> dataChangeListeners = commandListenersPerClientUrl.get(url);
                    if (MapUtils.isNotEmpty(dataChangeListeners)) {
                        for (Map.Entry<CommandListener, IZkDataListener> e : dataChangeListeners.entrySet()) {
                            subscribeCommandListener(url, e.getKey());
                        }
                    }
                }
                log.info("Re-registered the command listeners after a new zookeeper session");
            }
        } finally {
            listenerLock.unlock();
        }
    }

    @Override
    public List<String> getAllProviderForms() {
        return ZookeeperUtils.getChildrenNames(zkClient, PROVIDER_PATH);
    }

    @Override
    public Map<String, Map<String, List<AddressInfo>>> getAllProviders(String group) {
        return ZookeeperUtils.getAllProviders(zkClient, group);
    }

    /**
     * Register specified url info to zookeeper
     *
     * @param providerUrl provider url
     */
    @Override
    protected void doRegister(Url providerUrl) {
        try {
            providerLock.lock();
            // Remove old node in order to avoid using dirty data
            removeNode(providerUrl, ZookeeperStatusNode.ACTIVE);
            removeNode(providerUrl, ZookeeperStatusNode.INACTIVE);
            // Create data under 'inactive' node
            createNode(providerUrl, ZookeeperStatusNode.INACTIVE);
        } catch (Throwable e) {
            String errorMsg = MessageFormat.format("Failed to register [{0}] to zookeeper [{1}] with the error: {2}",
                    providerUrl, getRegistryUrl(), e.getMessage());
            throw new RuntimeException(errorMsg, e);
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * Register specified url info to zookeeper 'active' node
     *
     * @param providerUrl provider url
     */
    @Override
    protected void doActivate(Url providerUrl) {
        try {
            providerLock.lock();
            if (providerUrl == null) {
                // Register all provider urls to 'active' node if parameter url is null
                // Do NOT save Url.PARAM_ACTIVATED_TIME to activeProviderUrls
                activeProviderUrls.addAll(super.getRegisteredProviderUrls());

                for (Url u : super.getRegisteredProviderUrls()) {
                    if (!u.getBooleanOption(EXPOSED)) {
                        // Do NOT move non-exposed provider url to 'active' folder
                        log.info("Do NOT activate provider url [{}]", u.toSimpleString());
                        continue;
                    }
                    Url copy = u.copy();
                    // Add registered time parameter
//                    copy.addParameter(Url.PARAM_ACTIVATED_TIME, DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.format(new Date()));
                    // Remove the dirty data node
                    removeNode(copy, ZookeeperStatusNode.ACTIVE);
                    removeNode(copy, ZookeeperStatusNode.INACTIVE);
                    // Create data under 'active' node
                    createNode(copy, ZookeeperStatusNode.ACTIVE);
                }
            } else {
                // Do NOT save Url.PARAM_ACTIVATED_TIME to activeProviderUrls
                activeProviderUrls.add(providerUrl);

                Url copy = providerUrl.copy();
                // Remove the dirty data node
                removeNode(copy, ZookeeperStatusNode.ACTIVE);
                removeNode(copy, ZookeeperStatusNode.INACTIVE);
                // Create data under 'active' node
                createNode(copy, ZookeeperStatusNode.ACTIVE);
            }
        } finally {
            providerLock.unlock();
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
            providerLock.lock();
            if (providerUrl == null) {
                // Register all provider urls to 'inactive' node if parameter url is null
                activeProviderUrls.removeAll(getRegisteredProviderUrls());
                for (Url u : getRegisteredProviderUrls()) {
                    // Remove the dirty data node
                    removeNode(u, ZookeeperStatusNode.ACTIVE);
                    removeNode(u, ZookeeperStatusNode.INACTIVE);
                    // Create data under 'inactive' node
                    createNode(u, ZookeeperStatusNode.INACTIVE);
                }
            } else {
                activeProviderUrls.remove(providerUrl);
                // Remove the dirty data node
                removeNode(providerUrl, ZookeeperStatusNode.ACTIVE);
                removeNode(providerUrl, ZookeeperStatusNode.INACTIVE);
                // Create data under 'inactive' node
                createNode(providerUrl, ZookeeperStatusNode.INACTIVE);
            }
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * Delete specified directory
     *
     * @param providerUrl provider url
     * @param dir         directory
     */
    private void removeNode(Url providerUrl, ZookeeperStatusNode dir) {
        String nodePath = ZookeeperUtils.getProviderAddressFilePath(providerUrl, dir);
        if (zkClient.exists(nodePath)) {
            zkClient.delete(nodePath);
        }
    }

    /**
     * Create zookeeper persistent directory and ephemeral root node
     *
     * @param providerUrl url
     * @param statusNode  directory
     */
    private void createNode(Url providerUrl, ZookeeperStatusNode statusNode) {
        String activeStatusPath = ZookeeperUtils.getProviderStatusNodePath(providerUrl, statusNode);
        if (!zkClient.exists(activeStatusPath)) {
            // Create a persistent directory
            zkClient.createPersistent(activeStatusPath, true);
        }
        // Create a temporary file, which name is an address(host:port), which content is the full string of the url
        // And temporary files will be deleted automatically after closed zk session
        zkClient.createEphemeral(ZookeeperUtils.getProviderAddressFilePath(providerUrl, statusNode), providerUrl.toFullStr());
    }

    /**
     * Unregister specified url info from zookeeper
     *
     * @param providerUrl provider url
     */
    @Override
    protected void doUnregister(Url providerUrl) {
        try {
            providerLock.lock();
            removeNode(providerUrl, ZookeeperStatusNode.ACTIVE);
            removeNode(providerUrl, ZookeeperStatusNode.INACTIVE);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to unregister [{0}] from zookeeper [{1}] with the error: {2}", providerUrl, getRegistryUrl(), e.getMessage()), e);
        } finally {
            providerLock.unlock();
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
            String parentPath = ZookeeperUtils.getProviderStatusNodePath(clientUrl, ZookeeperStatusNode.ACTIVE);
            List<String> addrFiles = ZookeeperUtils.getChildrenNames(zkClient, parentPath);
            return readProviderUrls(parentPath, addrFiles);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to discover provider [{0}] from registry [{1}] with the error: {2}", clientUrl, getRegistryUrl(), e.getMessage()), e);
        }
    }

    /**
     * Read provider urls from address files' data
     *
     * @param dirName   zookeeper dir path, e.g. /infinity/default/org.infinity.app.common.service.AppService/active
     * @param fileNames address file names list, e.g. 172.25.11.111:26010,172.25.11.222:26010
     * @return provider urls
     */
    private List<Url> readProviderUrls(String dirName, List<String> fileNames) {
        List<Url> urls = new ArrayList<>();
        if (CollectionUtils.isEmpty(fileNames)) {
            return urls;
        }
        for (String fileName : fileNames) {
            String fullPath = dirName.concat(PATH_SEPARATOR).concat(fileName);
            String fileData = null;
            try {
                fileData = zkClient.readData(fullPath, true);
                log.debug("Read zookeeper file data: {}", fileData);
            } catch (Exception e) {
                log.warn("Failed to read the zookeeper file data!");
            }
            if (StringUtils.isNotBlank(fileData)) {
                try {
                    urls.add(Url.valueOf(fileData));
                } catch (Exception e) {
                    log.warn(MessageFormat.format("Found illegal zookeeper file data with path [{0}]", fullPath), e);
                }
            }
        }
        return urls;
    }

    @Override
    public List<String> discoverActiveProviderAddress(String providerPath) {
        List<String> addrFiles = new ArrayList<>();
        try {
            List<String> groups = ZookeeperUtils.getChildrenNames(zkClient, PROVIDER_PATH);
            if (CollectionUtils.isEmpty(groups)) {
                return addrFiles;
            }
            for (String group : groups) {
                String parentPath = ZookeeperUtils.getProviderStatusNodePath(group, providerPath, ZookeeperStatusNode.ACTIVE);
                addrFiles.addAll(CollectionUtils.emptyIfNull(ZookeeperUtils.getChildrenNames(zkClient, parentPath)));
            }
            return addrFiles;
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to discover providers from registry [{0}] with the error: {1}", getRegistryUrl(), e.getMessage()), e);
        }
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
    @EventSubscriber("providersChangeEvent")
    protected void subscribeServiceListener(Url clientUrl, ServiceListener serviceListener) {
        try {
            listenerLock.lock();
            Map<ServiceListener, IZkChildListener> childChangeListeners = providerListenersPerClientUrl.get(clientUrl);
            if (childChangeListeners == null) {
                providerListenersPerClientUrl.putIfAbsent(clientUrl, new ConcurrentHashMap<>());
                childChangeListeners = providerListenersPerClientUrl.get(clientUrl);
            }
            IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
            if (zkChildListener == null) {
                // child files change listener under specified directory
                zkChildListener = (dirName, currentChildren) -> {
                    @EventPublisher("providersChangeEvent")
                    List<String> fileNames = ListUtils.emptyIfNull(currentChildren);
                    serviceListener.onNotify(clientUrl, getRegistryUrl(), readProviderUrls(dirName, fileNames));
                    log.info("Provider files [{}] changed under path [{}]", String.join(",", fileNames), dirName);
                };
                childChangeListeners.putIfAbsent(serviceListener, zkChildListener);
            }

            try {
                // Remove dirty data
                removeNode(clientUrl, ZookeeperStatusNode.CLIENT);
                createNode(clientUrl, ZookeeperStatusNode.CLIENT);
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to remove or create the node with path [{0}]", ZookeeperUtils.getProviderAddressFilePath(clientUrl, ZookeeperStatusNode.CLIENT)), e);
            }

            String path = ZookeeperUtils.getProviderStatusNodePath(clientUrl, ZookeeperStatusNode.ACTIVE);
            // Bind the path with zookeeper child change listener, any the child list changes under the path will trigger the zkChildListener
            zkClient.subscribeChildChanges(path, zkChildListener);
            log.info("Subscribed the service listener for the path [{}]", ZookeeperUtils.getProviderAddressFilePath(clientUrl, ZookeeperStatusNode.ACTIVE));
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to subscribe service listeners for url [{}]", clientUrl), e);
        } finally {
            listenerLock.unlock();
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
            listenerLock.lock();
            Map<ServiceListener, IZkChildListener> childChangeListeners = providerListenersPerClientUrl.get(clientUrl);
            if (childChangeListeners == null) {
                return;
            }
            IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
            if (zkChildListener != null) {
                // Unbind the path with zookeeper child change listener
                zkClient.unsubscribeChildChanges(ZookeeperUtils.getProviderStatusNodePath(clientUrl, ZookeeperStatusNode.CLIENT), zkChildListener);
                childChangeListeners.remove(serviceListener);
            }
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to unsubscribe service listeners for url [{}]", clientUrl), e);
        } finally {
            listenerLock.unlock();
        }
    }

    /**
     * Monitor the specified zookeeper node linked to url whether the data have been changed, and it will invoke custom command listener if data change.
     *
     * @param clientUrl       client url to identify the zookeeper path
     * @param commandListener command listener
     */
    @Override
    @EventSubscriber("providerDataChangeEvent")
    protected void subscribeCommandListener(Url clientUrl, final CommandListener commandListener) {
        try {
            listenerLock.lock();
            Map<CommandListener, IZkDataListener> dataChangeListeners = commandListenersPerClientUrl.get(clientUrl);
            if (dataChangeListeners == null) {
                commandListenersPerClientUrl.putIfAbsent(clientUrl, new ConcurrentHashMap<>());
                dataChangeListeners = commandListenersPerClientUrl.get(clientUrl);
            }
            IZkDataListener zkDataListener = dataChangeListeners.get(commandListener);
            if (zkDataListener == null) {
                // Trigger user customized listener if data changes
                zkDataListener = new IZkDataListener() {
                    @Override
                    @EventPublisher("providerDataChangeEvent")
                    public void handleDataChange(String dataPath, Object data) {
                        commandListener.onNotify(clientUrl, (String) data);
                        log.info("Command data changed with current value {} under path [{}]", data.toString(), dataPath);
                    }

                    @Override
                    @EventPublisher("providerDataChangeEvent")
                    public void handleDataDeleted(String dataPath) {
                        commandListener.onNotify(clientUrl, null);
                        log.info("Command data deleted under path [{}]", dataPath);
                    }
                };
                dataChangeListeners.putIfAbsent(commandListener, zkDataListener);
            }

            String commandPath = ZookeeperUtils.getCommandPath(clientUrl);
            // Bind the path with zookeeper data change listener, any the data changes under the path will trigger the zkDataListener
            zkClient.subscribeDataChanges(commandPath, zkDataListener);
            log.info("Subscribed the command listener for the path [{}]", commandPath);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to subscribe command listeners for url [{}]", clientUrl), e);
        } finally {
            listenerLock.unlock();
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
            listenerLock.lock();
            Map<CommandListener, IZkDataListener> dataChangeListeners = commandListenersPerClientUrl.get(clientUrl);
            if (dataChangeListeners != null) {
                IZkDataListener zkDataListener = dataChangeListeners.get(commandListener);
                if (zkDataListener != null) {
                    zkClient.unsubscribeDataChanges(ZookeeperUtils.getCommandPath(clientUrl), zkDataListener);
                    dataChangeListeners.remove(commandListener);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to unsubscribe command listeners for url [{}]", clientUrl), e);
        } finally {
            listenerLock.unlock();
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
