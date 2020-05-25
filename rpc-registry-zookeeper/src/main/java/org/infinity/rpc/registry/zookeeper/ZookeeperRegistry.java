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
import org.infinity.rpc.core.registry.App;
import org.infinity.rpc.core.registry.CommandFailbackAbstractRegistry;
import org.infinity.rpc.core.registry.Url;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.registry.zookeeper.utils.ZookeeperUtils;
import org.infinity.rpc.utilities.annotation.Event;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;
import org.infinity.rpc.utilities.destory.Cleanable;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    /**
     * Used to resolve concurrency problems for subscribe or unsubscribe service listeners
     */
    private final Lock                                                           clientLock                    = new ReentrantLock();
    /**
     * Used to resolve concurrency problems for register or unregister providers
     */
    private final Lock                                                           providerLock                  = new ReentrantLock();
    private final Set<Url>                                                       activeProviderUrls            = new ConcurrentHashSet<>();
    private final Map<Url, ConcurrentHashMap<ServiceListener, IZkChildListener>> providerListenersPerClientUrl = new ConcurrentHashMap<>();
    private final Map<Url, ConcurrentHashMap<CommandListener, IZkDataListener>>  commandListenersPerClientUrl  = new ConcurrentHashMap<>();
    private       ZkClient                                                       zkClient;

    @Event
    public ZookeeperRegistry(Url registryUrl, ZkClient zkClient) {
        super(registryUrl);
        Validate.notNull(zkClient, "Zookeeper client must NOT be null!");
        this.zkClient = zkClient;
        IZkStateListener zkStateListener = new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
                // do nothing intentionally
            }

            /**
             * Called after the zookeeper session expired or a new zookeeper session, e,g restart zookeeper
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
    private void reregisterProviders() {
        Set<Url> allRegisteredServices = super.getRegisteredProviderUrls();
        if (CollectionUtils.isEmpty(allRegisteredServices)) {
            return;
        }
        try {
            providerLock.lock();
            for (Url url : super.getRegisteredProviderUrls()) {
                // re-register after a new session
                doRegister(url);
            }
            log.info("Re-registered all the providers after a reconnection to zookeeper");

            for (Url availableServiceUrl : activeProviderUrls) {
                if (!super.getRegisteredProviderUrls().contains(availableServiceUrl)) {
                    log.warn("Url [{}] has not been registered!", availableServiceUrl);
                    continue;
                }
                doActivate(availableServiceUrl);
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
    private void reregisterListeners() {
        try {
            clientLock.lock();
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
            clientLock.unlock();
        }
    }

    /**
     * Register application info to registry
     *
     * @param app application info
     */
    @Override
    public void registerApplication(App app) {
        // Create data under 'application' node
        createApplicationNode(app);
    }

    /**
     * Create zookeeper persistent directory and ephemeral root node
     *
     * @param app application info
     */
    private void createApplicationNode(App app) {
        String appNodePath = ZookeeperUtils.getApplicationPath(app.getName());
        if (!zkClient.exists(appNodePath)) {
            // Create a persistent directory
            zkClient.createPersistent(appNodePath, true);
        }

        for (Field field : App.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                String filePath = appNodePath + Url.PATH_SEPARATOR + field.getName();
                if (!zkClient.exists(filePath)) {
                    // Create temporary files and temporary files will be deleted automatically after closed zk session
                    zkClient.createEphemeral(filePath, field.get(app));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(MessageFormat.format("Failed to register application [{0}] to zookeeper [{1}] with the error: {2}", app.getName(), getRegistryUrl(), e.getMessage()), e);
            }
        }
    }

    /**
     * Register application provider info to registry
     *
     * @param app         application info
     * @param providerUrl provider url
     */
    @Override
    public void registerApplicationProvider(App app, Url providerUrl) {
        // Create data under 'application-providers/app-name' node
        createApplicationProviderNode(app, providerUrl);
    }

    /**
     * Create zookeeper persistent directory and ephemeral root node
     *
     * @param app         application info
     * @param providerUrl url
     */
    private void createApplicationProviderNode(App app, Url providerUrl) {
        String appNodePath = ZookeeperUtils.getApplicationProviderPath(app.getName());
        if (!zkClient.exists(appNodePath)) {
            // Create a persistent directory
            zkClient.createPersistent(appNodePath, true);
        }

        String filePath = appNodePath + Url.PATH_SEPARATOR + providerUrl.getPath();
        // Create a temporary file which content is the full string of the url
        // And temporary files will be deleted automatically after closed zk session
        // Append multiple provider url to file contents
        zkClient.createEphemeral(filePath, providerUrl.toFullStr());
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
            // Create data under 'offline' node
            createNode(providerUrl, ZookeeperStatusNode.INACTIVE);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to register [{0}] to zookeeper [{1}] with the error: {2}", providerUrl, getRegistryUrl(), e.getMessage()), e);
        } finally {
            providerLock.unlock();
        }
    }

    /**
     * Register specified url info to zookeeper 'online' node
     *
     * @param providerUrl provider url
     */
    @Override
    protected void doActivate(Url providerUrl) {
        try {
            providerLock.lock();
            if (providerUrl == null) {
                // Register all provider urls to 'online' node if parameter url is null
                activeProviderUrls.addAll(super.getRegisteredProviderUrls());
                for (Url u : super.getRegisteredProviderUrls()) {
                    // Remove the dirty data node
                    removeNode(u, ZookeeperStatusNode.ACTIVE);
                    removeNode(u, ZookeeperStatusNode.INACTIVE);
                    // Create data under 'online' node
                    createNode(u, ZookeeperStatusNode.ACTIVE);
                }
            } else {
                activeProviderUrls.add(providerUrl);
                // Remove the dirty data node
                removeNode(providerUrl, ZookeeperStatusNode.ACTIVE);
                removeNode(providerUrl, ZookeeperStatusNode.INACTIVE);
                // Create data under 'online' node
                createNode(providerUrl, ZookeeperStatusNode.ACTIVE);
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
                // Register all provider urls to 'offline' node if parameter url is null
                activeProviderUrls.removeAll(getRegisteredProviderUrls());
                for (Url u : getRegisteredProviderUrls()) {
                    // Remove the dirty data node
                    removeNode(u, ZookeeperStatusNode.ACTIVE);
                    removeNode(u, ZookeeperStatusNode.INACTIVE);
                    // Create data under 'offline' node
                    createNode(u, ZookeeperStatusNode.INACTIVE);
                }
            } else {
                activeProviderUrls.remove(providerUrl);
                // Remove the dirty data node
                removeNode(providerUrl, ZookeeperStatusNode.ACTIVE);
                removeNode(providerUrl, ZookeeperStatusNode.INACTIVE);
                // Create data under 'offline' node
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
        String nodePath = ZookeeperUtils.getAddressPath(providerUrl, dir);
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
        String activeStatusPath = ZookeeperUtils.getStatusNodePath(providerUrl, statusNode);
        if (!zkClient.exists(activeStatusPath)) {
            // Create a persistent directory
            zkClient.createPersistent(activeStatusPath, true);
        }
        // Create a temporary file, which name is an address(host:port), which content is the full string of the url
        // And temporary files will be deleted automatically after closed zk session
        zkClient.createEphemeral(ZookeeperUtils.getAddressPath(providerUrl, statusNode), providerUrl.toFullStr());
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
            String parentPath = ZookeeperUtils.getStatusNodePath(clientUrl, ZookeeperStatusNode.ACTIVE);
            List<String> addrFiles = ZookeeperUtils.getChildren(zkClient, parentPath);
            return readProviderUrls(addrFiles, parentPath, clientUrl);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to discover provider [{0}] from registry [{1}] with the error: {2}", clientUrl, getRegistryUrl(), e.getMessage()), e);
        }
    }

    /**
     * Read provider urls from address files' data
     *
     * @param addrFiles address file list
     * @param path      zookeeper path
     * @param clientUrl client url
     * @return provider urls
     */
    private List<Url> readProviderUrls(List<String> addrFiles, String path, Url clientUrl) {
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
//                newurl = clientUrl.copy();
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

    @Override
    public List<String> discoverActiveProviderAddress(String providerPath) {
        List<String> addrFiles = new ArrayList<>();
        try {
            List<String> groups = ZookeeperUtils.getGroups(zkClient);
            if (CollectionUtils.isEmpty(groups)) {
                return addrFiles;
            }
            for (String group : groups) {
                String parentPath = ZookeeperUtils.getStatusNodePath(group, providerPath, ZookeeperStatusNode.ACTIVE);
                addrFiles.addAll(CollectionUtils.emptyIfNull(ZookeeperUtils.getChildren(zkClient, parentPath)));
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
    @Event
    protected void subscribeServiceListener(Url clientUrl, ServiceListener serviceListener) {
        try {
            clientLock.lock();
            Map<ServiceListener, IZkChildListener> childChangeListeners = providerListenersPerClientUrl.get(clientUrl);
            if (childChangeListeners == null) {
                providerListenersPerClientUrl.putIfAbsent(clientUrl, new ConcurrentHashMap<>());
                childChangeListeners = providerListenersPerClientUrl.get(clientUrl);
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
                removeNode(clientUrl, ZookeeperStatusNode.CLIENT);
                createNode(clientUrl, ZookeeperStatusNode.CLIENT);
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to remove or create the node with path [{0}]", ZookeeperUtils.getAddressPath(clientUrl, ZookeeperStatusNode.CLIENT)), e);
            }

            String path = ZookeeperUtils.getStatusNodePath(clientUrl, ZookeeperStatusNode.ACTIVE);
            // Bind the path with zookeeper child change listener, any the child list changes under the path will trigger the zkChildListener
            zkClient.subscribeChildChanges(path, zkChildListener);
            log.info("Subscribed the service listener for the path [{}]", ZookeeperUtils.getAddressPath(clientUrl, ZookeeperStatusNode.ACTIVE));
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to subscribe service listeners for url [{}]", clientUrl), e);
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
            Map<ServiceListener, IZkChildListener> childChangeListeners = providerListenersPerClientUrl.get(clientUrl);
            if (childChangeListeners == null) {
                return;
            }
            IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
            if (zkChildListener != null) {
                // Unbind the path with zookeeper child change listener
                zkClient.unsubscribeChildChanges(ZookeeperUtils.getStatusNodePath(clientUrl, ZookeeperStatusNode.CLIENT), zkChildListener);
                childChangeListeners.remove(serviceListener);
            }
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to unsubscribe service listeners for url [{}]", clientUrl), e);
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
            log.info("Subscribed the command listener for the path [{}]", commandPath);
        } catch (Throwable e) {
            throw new RuntimeException(MessageFormat.format("Failed to subscribe command listeners for url [{}]", clientUrl), e);
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
