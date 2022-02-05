//package org.infinity.luix.registry.zookeeper;
//
//import lombok.extern.slf4j.Slf4j;
//import org.I0Itec.zkclient.IZkChildListener;
//import org.I0Itec.zkclient.IZkStateListener;
//import org.I0Itec.zkclient.ZkClient;
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.collections4.ListUtils;
//import org.apache.commons.collections4.MapUtils;
//import org.apache.commons.lang3.Validate;
//import org.apache.zookeeper.Watcher;
//import org.infinity.luix.core.exception.impl.RpcFrameworkException;
//import org.infinity.luix.core.listener.client.ConsumerProcessable;
//import org.infinity.luix.core.listener.server.ProviderListener;
//import org.infinity.luix.core.registry.FailbackAbstractRegistry;
//import org.infinity.luix.core.url.Url;
//import org.infinity.luix.utilities.annotation.EventPublisher;
//import org.infinity.luix.utilities.annotation.EventSubscriber;
//import org.infinity.luix.utilities.collection.ConcurrentHashSet;
//import org.infinity.luix.utilities.destory.Destroyable;
//
//import javax.annotation.concurrent.ThreadSafe;
//import java.text.MessageFormat;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.locks.Lock;
//import java.util.concurrent.locks.ReentrantLock;
//
//import static org.infinity.luix.registry.zookeeper.utils.ZookeeperUtils.*;
//
///**
// * Zookeeper registry implementation used to subscribe, unsubscribe, register or deregister data.
// * Zookeeper has three vital listeners:
// * - IZkStateListener: It will be triggered when a new zk session created.
// * - IZkDataListener: It will be triggered when the file data has been changed.
// * - IZkChildListener: It will be triggered when the files under the node has been changed.
// */
//@Slf4j
//@ThreadSafe
//public class ZookeeperRegistry extends FailbackAbstractRegistry implements Destroyable {
//    private final ZkClient                                                        zkClient;
//    /**
//     * Used to resolve concurrency problems for subscribe or unsubscribe service listeners
//     */
//    private final Lock                                                            listenerLock                    = new ReentrantLock();
//    /**
//     * Used to resolve concurrency problems for register or deregister providers
//     */
//    private final Lock                                                            providerLock                    = new ReentrantLock();
//    private final Set<Url>                                                        activeProviderUrls              = new ConcurrentHashSet<>();
//    private final Map<Url, ConcurrentHashMap<ProviderListener, IZkChildListener>> providerListenersPerConsumerUrl = new ConcurrentHashMap<>();
//    private final Map<String, IZkChildListener>                                   zkChildListenerPerInterfaceName = new ConcurrentHashMap<>();
//
//    public ZookeeperRegistry(ZkClient zkClient, Url registryUrl) {
//        super(registryUrl);
//        Validate.notNull(zkClient, "Zookeeper client must NOT be null!");
//        this.zkClient = zkClient;
//        IZkStateListener zkStateListener = new IZkStateListener() {
//            @Override
//            public void handleStateChanged(Watcher.Event.KeeperState state) {
//                // do nothing intentionally
//            }
//
//            /**
//             * Called after the zookeeper session expired or a new zookeeper session, e,g restart zookeeper
//             * You would have to re-create any ephemeral nodes here.
//             */
//            @Override
//            @EventPublisher({"providersChangeEvent", "providerDataChangeEvent"})
//            public void handleNewSession() {
//                log.info("Created a new zookeeper session");
//                reregisterProviders();
//                reregisterListeners();
//            }
//
//            @Override
//            public void handleSessionEstablishmentError(Throwable error) {
//                // do nothing intentionally
//            }
//        };
//        // Subscribe zk listener
//        zkClient.subscribeStateChanges(zkStateListener);
//    }
//
//    /**
//     * Default access modifier for unit test
//     *
//     * @return provider listeners
//     */
//    Map<Url, ConcurrentHashMap<ProviderListener, IZkChildListener>> getProviderListenersPerConsumerUrl() {
//        return providerListenersPerConsumerUrl;
//    }
//
//    /**
//     * Re-register the providers after a new zookeeper session
//     * e.g, Zookeeper was shutdown after the infinity application startup, then zookeeper startup again will cause a new session.
//     */
//    protected void reregisterProviders() {
//        Set<Url> allRegisteredProviders = super.getRegisteredProviderUrls();
//        if (CollectionUtils.isEmpty(allRegisteredProviders)) {
//            return;
//        }
//
//        providerLock.lock();
//        try {
//            for (Url url : allRegisteredProviders) {
//                // Re-register after a new session
//                doRegister(url);
//            }
//            log.info("Re-registered all the providers after a reconnection to zookeeper");
//
//            for (Url activeProviderUrl : activeProviderUrls) {
//                // Only registered provider can be re-registered
//                if (!allRegisteredProviders.contains(activeProviderUrl)) {
//                    log.warn("Url [{}] has not been registered!", activeProviderUrl);
//                    continue;
//                }
//                doActivate(activeProviderUrl);
//            }
//            log.info("Re-registered the provider urls after a new zookeeper session");
//        } finally {
//            providerLock.unlock();
//        }
//    }
//
//    /**
//     * Re-register the provider and command listeners after a new zookeeper session
//     * e.g, Zookeeper was shutdown after the infinity application startup, then zookeeper startup again will cause a new session.
//     */
//    protected void reregisterListeners() {
//        listenerLock.lock();
//        try {
//            if (MapUtils.isNotEmpty(providerListenersPerConsumerUrl)) {
//                for (Map.Entry<Url, ConcurrentHashMap<ProviderListener, IZkChildListener>> entry : providerListenersPerConsumerUrl.entrySet()) {
//                    Url url = entry.getKey();
//                    ConcurrentHashMap<ProviderListener, IZkChildListener> childChangeListeners = providerListenersPerConsumerUrl.get(url);
//                    if (MapUtils.isNotEmpty(childChangeListeners)) {
//                        for (Map.Entry<ProviderListener, IZkChildListener> e : childChangeListeners.entrySet()) {
//                            subscribeListener(url, e.getKey());
//                        }
//                    }
//                }
//                log.info("Re-registered the provider listeners after a new zookeeper session");
//            }
//        } finally {
//            listenerLock.unlock();
//        }
//    }
//
//    /**
//     * Register specified url info to zookeeper
//     *
//     * @param url provider or consumer url
//     */
//    @Override
//    protected void doRegister(Url url) {
//        if (url.isProvider()) {
//            providerLock.lock();
//            try {
//                // Remove old node in order to avoid using dirty data
//                removeNode(url, StatusDir.ACTIVE);
//                removeNode(url, StatusDir.INACTIVE);
//                // Create data under 'inactive' node
//                createNode(url, StatusDir.INACTIVE);
//            } catch (Throwable e) {
//                String msg = String.format("Failed to register [%s] to zookeeper [%s] with the error: %s",
//                        url, getRegistryUrl(), e.getMessage());
//                throw new RpcFrameworkException(msg, e);
//            } finally {
//                providerLock.unlock();
//            }
//        } else {
//            createConsumingNode(url);
//        }
//    }
//
//    /**
//     * Deregister specified url info from zookeeper
//     *
//     * @param url provider or consumer url
//     */
//    @Override
//    protected void doDeregister(Url url) {
//        if (url.isProvider()) {
//            providerLock.lock();
//            try {
//                removeNode(url, StatusDir.ACTIVE);
//                removeNode(url, StatusDir.INACTIVE);
//            } catch (Throwable e) {
//                String msg = String.format("Failed to deregister [%s] from zookeeper [%s] with the error: %s",
//                        url, getRegistryUrl(), e.getMessage());
//                throw new RpcFrameworkException(msg, e);
//            } finally {
//                providerLock.unlock();
//            }
//        } else {
//            try {
//                // Remove dirty data
//                removeNode(url, StatusDir.CONSUMING);
//            } catch (Exception e) {
//                log.warn(MessageFormat.format("Failed to remove the node for the path [{0}]",
//                        getProviderFilePath(url, StatusDir.CONSUMING)), e);
//            }
//        }
//    }
//
//    /**
//     * Register specified url info to zookeeper 'active' node
//     *
//     * @param url provider or consumer url
//     */
//    @Override
//    protected void doActivate(Url url) {
//        if (url.isProvider()) {
//            providerLock.lock();
//            try {
//                if (url == null) {
//                    // Register all provider urls to 'active' node if parameter url is null
//                    // Do NOT save Url.PARAM_ACTIVATED_TIME to activeProviderUrls
//                    activeProviderUrls.addAll(super.getRegisteredProviderUrls());
//
//                    for (Url u : super.getRegisteredProviderUrls()) {
//                        Url copy = u.copy();
//                        // Remove the dirty data node
//                        removeNode(copy, StatusDir.ACTIVE);
//                        removeNode(copy, StatusDir.INACTIVE);
//                        // Create data under 'active' node
//                        createNode(copy, StatusDir.ACTIVE);
//                    }
//                } else {
//                    // Do NOT save Url.PARAM_ACTIVATED_TIME to activeProviderUrls
//                    activeProviderUrls.add(url);
//
//                    Url copy = url.copy();
//                    // Remove the dirty data node
//                    removeNode(copy, StatusDir.ACTIVE);
//                    removeNode(copy, StatusDir.INACTIVE);
//                    // Create data under 'active' node
//                    createNode(copy, StatusDir.ACTIVE);
//                }
//            } finally {
//                providerLock.unlock();
//            }
//        }
//    }
//
//    /**
//     * Register specified url info to zookeeper inactive node
//     *
//     * @param url provider or consumer url
//     */
//    @Override
//    protected void doDeactivate(Url url) {
//        if (url.isProvider()) {
//            providerLock.lock();
//            try {
//                if (url == null) {
//                    // Register all provider urls to 'inactive' node if parameter url is null
//                    activeProviderUrls.removeAll(getRegisteredProviderUrls());
//                    for (Url u : getRegisteredProviderUrls()) {
//                        // Remove the dirty data node
//                        removeNode(u, StatusDir.ACTIVE);
//                        removeNode(u, StatusDir.INACTIVE);
//                        // Create data under 'inactive' node
//                        createNode(u, StatusDir.INACTIVE);
//                    }
//                } else {
//                    activeProviderUrls.remove(url);
//                    // Remove the dirty data node
//                    removeNode(url, StatusDir.ACTIVE);
//                    removeNode(url, StatusDir.INACTIVE);
//                    // Create data under 'inactive' node
//                    createNode(url, StatusDir.INACTIVE);
//                }
//            } finally {
//                providerLock.unlock();
//            }
//        }
//    }
//
//    /**
//     * Create zookeeper persistent directory and ephemeral root node
//     *
//     * @param url       url
//     * @param statusDir status directory
//     */
//    private void createNode(Url url, StatusDir statusDir) {
//        String statusDirPath = getStatusDirPath(url.getPath(), statusDir);
//        if (!zkClient.exists(statusDirPath)) {
//            // Create a persistent directory
//            zkClient.createPersistent(statusDirPath, true);
//        }
//        // Create a temporary file, whose name pattern is an address(host:port:form),
//        // and whose content is the full string of the url,
//        // And temporary files will be deleted automatically after closed zk session
//        zkClient.createEphemeral(getProviderFilePath(url, statusDir), url.toFullStr());
//    }
//
//    private void createConsumingNode(Url consumerUrl) {
//        try {
//            // Remove dirty data
//            removeNode(consumerUrl, StatusDir.CONSUMING);
//            // Create consumer url data under 'consuming' node
//            createNode(consumerUrl, StatusDir.CONSUMING);
//        } catch (Exception e) {
//            log.warn(MessageFormat.format("Failed to remove or create the node for the path [{0}]",
//                    getProviderFilePath(consumerUrl, StatusDir.CONSUMING)), e);
//        }
//    }
//
//    /**
//     * Delete specified provider file
//     *
//     * @param url       url
//     * @param statusDir status directory
//     */
//    private void removeNode(Url url, StatusDir statusDir) {
//        String filePath = getProviderFilePath(url, statusDir);
//        if (zkClient.exists(filePath)) {
//            zkClient.delete(filePath);
//        }
//    }
//
//    /**
//     * Discover active providers' address file name list under single node or cluster environment
//     *
//     * @param consumerUrl consumer url
//     * @return provider urls
//     */
//    @Override
//    public List<Url> discoverProviders(Url consumerUrl) {
//        try {
//            return readUrls(zkClient, consumerUrl.getPath(), StatusDir.ACTIVE);
//        } catch (Throwable e) {
//            String msg = String.format("Failed to discover provider [%s] from registry [%s] with the error: %s",
//                    consumerUrl, getRegistryUrl(), e.getMessage());
//            throw new RpcFrameworkException(msg, e);
//        }
//    }
//
//    /**
//     * Monitor the specified zookeeper node linked to url whether the child nodes have been changed,
//     * and it will invoke custom service listener if child nodes change.
//     *
//     * @param consumerUrl      consumer url to identify the zookeeper path
//     * @param providerListener service listener
//     */
//    @Override
//    protected void subscribeListener(Url consumerUrl, ProviderListener providerListener) {
//        listenerLock.lock();
//        try {
//            doSubscribeProviderListener(consumerUrl, providerListener);
//        } catch (Throwable e) {
//            String msg = String.format("Failed to subscribe provider listeners for url [%s]", consumerUrl);
//            throw new RpcFrameworkException(msg, e);
//        } finally {
//            listenerLock.unlock();
//        }
//    }
//
//    private void doSubscribeProviderListener(Url consumerUrl, ProviderListener providerListener) {
//        String activeDirPath = getStatusDirPath(consumerUrl.getPath(), StatusDir.ACTIVE);
//        IZkChildListener zkChildListener = getZkChildListener(consumerUrl, providerListener);
//        // Bind the path with zookeeper child change listener, any the child list changes under the path will trigger the zkChildListener
//        zkClient.subscribeChildChanges(activeDirPath, zkChildListener);
//        log.info("Subscribed the provider listener for the path [{}]", getProviderFilePath(consumerUrl, StatusDir.ACTIVE));
//    }
//
//    @EventSubscriber("providersChangeEvent")
//    private IZkChildListener getZkChildListener(Url consumerUrl, ProviderListener providerListener) {
//        Map<ProviderListener, IZkChildListener> childChangeListeners = providerListenersPerConsumerUrl.get(consumerUrl);
//        if (childChangeListeners == null) {
//            providerListenersPerConsumerUrl.putIfAbsent(consumerUrl, new ConcurrentHashMap<>(16));
//            childChangeListeners = providerListenersPerConsumerUrl.get(consumerUrl);
//        }
//        IZkChildListener zkChildListener = childChangeListeners.get(providerListener);
//        if (zkChildListener == null) {
//            // Child files change listener under specified directory
//            zkChildListener = (dirName, currentChildren) -> {
//                @EventPublisher("providersChangeEvent")
//                List<String> fileNames = ListUtils.emptyIfNull(currentChildren);
//                List<Url> providerUrls = readUrls(zkClient, dirName, fileNames);
//                providerListener.onNotify(getRegistryUrl(), consumerUrl.getPath(), providerUrls);
//                // Notify all consumers
//                Optional.ofNullable(consumersListener).ifPresent(l -> l.onNotify(getRegistryUrl(), consumerUrl.getPath(), providerUrls));
//                log.info("Provider files [{}] changed under path [{}]", String.join(",", fileNames), dirName);
//            };
//            childChangeListeners.putIfAbsent(providerListener, zkChildListener);
//        }
//        return zkChildListener;
//    }
//
//    /**
//     * Unsubscribe the service listener from the specified zookeeper node
//     *
//     * @param consumerUrl      consumer url to identify the zookeeper path
//     * @param providerListener service listener
//     */
//    @Override
//    protected void unsubscribeProviderListener(Url consumerUrl, ProviderListener providerListener) {
//        listenerLock.lock();
//        try {
//            Map<ProviderListener, IZkChildListener> childChangeListeners = providerListenersPerConsumerUrl.get(consumerUrl);
//            if (childChangeListeners == null) {
//                return;
//            }
//            IZkChildListener zkChildListener = childChangeListeners.get(providerListener);
//            if (zkChildListener != null) {
//                // Unbind the path with zookeeper child change listener
//                zkClient.unsubscribeChildChanges(getStatusDirPath(consumerUrl.getPath(), StatusDir.CONSUMING), zkChildListener);
//                childChangeListeners.remove(providerListener);
//            }
//        } catch (Throwable e) {
//            String msg = String.format("Failed to unsubscribe provider listeners for url [%s]", consumerUrl);
//            throw new RpcFrameworkException(msg, e);
//        } finally {
//            listenerLock.unlock();
//        }
//    }
//
//    @Override
//    public List<Url> getAllProviderUrls() {
//        List<String> paths = getChildrenNames(zkClient, FULL_PATH_PROVIDER);
//        if (CollectionUtils.isEmpty(paths)) {
//            return Collections.emptyList();
//        }
//        List<Url> urls = new ArrayList<>();
//        paths.forEach(path -> {
//            urls.addAll(readUrls(zkClient, path, StatusDir.ACTIVE));
//            urls.addAll(readUrls(zkClient, path, StatusDir.INACTIVE));
//        });
//        return urls;
//    }
//
//    @Override
//    public void subscribeAllConsumerChanges(ConsumerProcessable consumerProcessor) {
//        listenerLock.lock();
//        getRegisteredConsumerUrls().forEach(url -> {
//            try {
//                IZkChildListener zkChildListener = zkChildListenerPerInterfaceName.get(url.getPath());
//                if (zkChildListener == null) {
//                    // Child files change listener under specified directory
//                    zkChildListener = (dirName, currentChildren) -> {
//                        @EventPublisher("consumersChangeEvent")
//                        List<String> fileNames = ListUtils.emptyIfNull(currentChildren);
//                        consumerProcessor.process(getRegistryUrl(), url.getPath(), readUrls(zkClient, dirName, fileNames));
//                        log.info("Consumer files [{}] changed under path [{}]", String.join(",", fileNames), dirName);
//                    };
//                    zkChildListenerPerInterfaceName.putIfAbsent(url.getPath(), zkChildListener);
//                }
//                String consumingDirPath = getStatusDirPath(url.getPath(), StatusDir.CONSUMING);
//                // Bind the path with zookeeper child change listener, any the child list changes under the path will trigger the zkChildListener
//                zkClient.subscribeChildChanges(consumingDirPath, zkChildListener);
//                log.info("Subscribed the service listener for the path [{}]", consumingDirPath);
//
//                List<String> fileNames = ListUtils.emptyIfNull(getChildrenNames(zkClient, consumingDirPath));
//                consumerProcessor.process(getRegistryUrl(), url.getPath(), readUrls(zkClient, consumingDirPath, fileNames));
//            } catch (Throwable e) {
//                String msg = String.format("Failed to subscribe consumer listeners for the path [%s]", url.getPath());
//                throw new RpcFrameworkException(msg, e);
//            }
//        });
//        listenerLock.unlock();
//    }
//
//    /**
//     * Do cleanup stuff
//     */
//    @Override
//    public void destroy() {
//        this.zkClient.close();
//    }
//}
