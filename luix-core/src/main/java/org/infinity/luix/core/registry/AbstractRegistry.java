package org.infinity.luix.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.luix.core.exception.impl.RpcConfigException;
import org.infinity.luix.core.listener.GlobalProviderDiscoveryListener;
import org.infinity.luix.core.listener.ProviderDiscoveryListener;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.collection.ConcurrentHashSet;
import org.infinity.luix.utilities.concurrent.NotThreadSafe;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.infinity.luix.core.constant.ProtocolConstants.CODEC;

/**
 * Abstract registry
 */
@Slf4j
@NotThreadSafe
public abstract class AbstractRegistry implements Registry {
    /**
     * Registry url
     */
    protected       Url                                                       registryUrl;
    /**
     * Registered provider urls cache
     */
    private final   Set<Url>                                                  registeredProviderUrls = new ConcurrentHashSet<>();
    /**
     * Registered consumer urls cache
     */
    private final   Set<Url>                                                  registeredConsumerUrls = new ConcurrentHashSet<>();
    /**
     * Key: path
     * Value: provider urls
     */
    protected final Map<String, List<Url>>                                    path2ProviderUrls      = new ConcurrentHashMap<>();
    /**
     * One consumer can subscribe multiple listeners
     * Key: path
     * Value: consumer listeners
     */
    protected final Map<String, ConcurrentHashSet<ProviderDiscoveryListener>> path2Listeners         = new ConcurrentHashMap<>();
    /**
     * Provider changes notification thread pool
     */
    protected final ThreadPoolExecutor                                        notificationThreadPool;
    /**
     * Listener used to handle the subscribed event for all consumers
     */
    protected       GlobalProviderDiscoveryListener                           globalProviderDiscoveryListener;

    public AbstractRegistry(Url registryUrl) {
        Validate.notNull(registryUrl, "Registry url must NOT be null!");
        this.registryUrl = registryUrl;
        this.notificationThreadPool = createNotificationThreadPool();
    }

    private ThreadPoolExecutor createNotificationThreadPool() {
        return new ThreadPoolExecutor(10, 30, 30 * 1_000,
                TimeUnit.MILLISECONDS, createWorkQueue(), new ThreadPoolExecutor.AbortPolicy());
    }

    private BlockingQueue<Runnable> createWorkQueue() {
        return new ArrayBlockingQueue<>(20_000);
    }

    /**
     * Get registry url
     *
     * @return registry url
     */
    @Override
    public Url getRegistryUrl() {
        return registryUrl;
    }

    /**
     * Get the registered provider urls cache
     *
     * @return provider urls
     */
    @Override
    public Set<Url> getRegisteredProviderUrls() {
        return registeredProviderUrls;
    }

    /**
     * Get the registered consumer urls cache
     *
     * @return consumer urls
     */
    @Override
    public Set<Url> getRegisteredConsumerUrls() {
        return registeredConsumerUrls;
    }

    @Override
    public String getType() {
        return registryUrl.getProtocol();
    }

    /**
     * Register a provider or consumer url to registry
     *
     * @param url provider or consumer url
     */
    @Override
    public void register(Url url) {
        Validate.notNull(url, "Url must NOT be null!");
        doRegister(removeUnnecessaryParams(url.copy()));
        log.info("Registered the url [{}] to registry [{}]", url, registryUrl.getIdentity());
        // Added it to the cache after registered
        if (url.isProvider()) {
            registeredProviderUrls.add(url);
        } else if (url.isConsumer()) {
            registeredConsumerUrls.add(url);
        } else {
            throw new RpcConfigException("Url must be provider or consumer!");
        }
    }

    /**
     * Deregister the provider or consumer url from registry
     *
     * @param url provider or consumer url
     */
    @Override
    public void deregister(Url url) {
        Validate.notNull(url, "Url must NOT be null!");
        doDeregister(removeUnnecessaryParams(url.copy()));
        log.info("Deregistered the url [{}] from registry [{}] by using [{}]", url, registryUrl.getIdentity(),
                this.getClass().getSimpleName());
        // Removed it from the container after de-registered
        if (url.isProvider()) {
            registeredProviderUrls.remove(url);
        } else if (url.isConsumer()) {
            registeredConsumerUrls.remove(url);
        } else {
            throw new RpcConfigException("Url must be provider or consumer!");
        }
    }

    /**
     * Activate the url from registry
     *
     * @param url provider or consumer url
     */
    @Override
    public void activate(Url url) {
        if (url != null) {
            doActivate(removeUnnecessaryParams(url.copy()));
            log.info("Activated the url [{}] on registry [{}] by using [{}]", url, registryUrl.getIdentity(),
                    this.getClass().getSimpleName());
        } else {
            doActivate(null);
        }
    }

    /**
     * Deactivate the url from registry
     *
     * @param url provider or consumer url
     */
    @Override
    public void deactivate(Url url) {
        if (url != null) {
            doDeactivate(removeUnnecessaryParams(url.copy()));
            log.info("Deactivated the url [{}] on registry [{}] by using [{}]", url, registryUrl.getIdentity(),
                    this.getClass().getSimpleName());
        } else {
            doDeactivate(null);
        }
    }

    /**
     * Remove the unnecessary url param to register to registry in order to not to be seen by consumer
     *
     * @param url url
     */
    private Url removeUnnecessaryParams(Url url) {
        // codec parameter can not be registered to registry,
        // because client side may could not request successfully if client side does not have the codec.
        url.getOptions().remove(CODEC);
        return url;
    }

    /**
     * Subscribe the listener to specified consumer url
     *
     * @param consumerUrl consumer url
     * @param listener    listener
     */
    @Override
    public void subscribe(Url consumerUrl, ProviderDiscoveryListener listener) {
        Validate.notNull(consumerUrl, "Consumer url must NOT be null!");
        Validate.notNull(listener, "Consumer listener must NOT be null!");

        doSubscribe(consumerUrl, listener);
        log.info("Subscribed the url [{}] to listener [{}] by using [{}]", registryUrl.getIdentity(), listener,
                this.getClass().getSimpleName());
    }

    /**
     * Unsubscribe the listener from specified consumer url
     *
     * @param consumerUrl provider url
     * @param listener    listener
     */
    @Override
    public void unsubscribe(Url consumerUrl, ProviderDiscoveryListener listener) {
        Validate.notNull(consumerUrl, "Consumer url must NOT be null!");
        Validate.notNull(listener, "Consumer listener must NOT be null!");

        doUnsubscribe(consumerUrl, listener);
        log.info("Unsubscribed the url [{}] from listener [{}] by using [{}]", registryUrl.getIdentity(), listener,
                this.getClass().getSimpleName());
    }

    /**
     * Subscribe all consumers events to specified listener
     *
     * @param listener consumers listener
     */
    @Override
    public void subscribe(GlobalProviderDiscoveryListener listener) {
        this.globalProviderDiscoveryListener = listener;
    }

    /**
     * Unsubscribe all consumers events from specified listener
     *
     * @param listener consumers listener
     */
    @Override
    public void unsubscribe(GlobalProviderDiscoveryListener listener) {
        this.globalProviderDiscoveryListener = null;
    }

    /**
     * Discover 'active' the provider urls of the specified consumer, including 'inactive' urls
     *
     * @param consumerUrl        consumer url
     * @param onlyFetchFromCache if true, only fetch from cache
     * @return provider urls
     */
    @Override
    public List<Url> discoverActive(Url consumerUrl, boolean onlyFetchFromCache) {
        Validate.notNull(consumerUrl, "Consumer url must NOT be null!");

        List<Url> cachedProviderUrls = path2ProviderUrls.get(consumerUrl.getPath());
        if (CollectionUtils.isNotEmpty(cachedProviderUrls)) {
            // Get all the provider urls from cache
            return cachedProviderUrls.stream().map(Url::copy).collect(Collectors.toList());
        } else if (onlyFetchFromCache) {
            return Collections.emptyList();
        } else {
            // Discover the provider urls from registry if local cache does not exist
            List<Url> providerUrls = doDiscover(consumerUrl);
            if (CollectionUtils.isNotEmpty(providerUrls)) {
                // Make a copy and add to results
                return providerUrls.stream().map(Url::copy).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }

    /**
     * Discover the provider urls from registry
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    protected List<Url> doDiscover(Url consumerUrl) {
        List<Url> providerUrls = discoverProviders(consumerUrl);
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            log.info("Discovered the provider urls [{}] for consumer url [{}]", providerUrls, consumerUrl);
        } else {
            log.info("No providers found for consumer url [{}]!", consumerUrl);
        }
        return providerUrls;
    }

    /**
     * Subscribe the listener to specified consumer url
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    protected void doSubscribe(Url consumerUrl, ProviderDiscoveryListener listener) {
        path2Listeners.computeIfAbsent(consumerUrl.getPath(), k -> new ConcurrentHashSet<>()).add(listener);

        subscribeListener(consumerUrl, listener);

        // Discover active providers at subscribe time
        List<Url> providerUrls = discoverActive(consumerUrl, false);
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            updateAndNotify(consumerUrl.getPath(), providerUrls);
        }
        log.info("Subscribed the listener for the consumer url [{}]", consumerUrl);
    }

    /**
     * Notify the discovered provider urls to consumers
     *
     * @param path         interface name
     * @param providerUrls provider urls
     */
    protected void updateAndNotify(String path, List<Url> providerUrls) {
        notificationThreadPool.execute(() -> {
            synchronized (path.intern()) {
                if (CollectionUtils.isEmpty(providerUrls)) {
                    path2ProviderUrls.remove(path);
                } else {
                    path2ProviderUrls.put(path, providerUrls);
                }

                if (CollectionUtils.isNotEmpty(path2Listeners.get(path))) {
                    // Notify to specified consumers
                    path2Listeners.get(path).forEach(listener -> listener.onNotify(registryUrl, path, providerUrls));
                }

                // Notify to all consumers
                Optional.ofNullable(globalProviderDiscoveryListener).ifPresent(l -> l.onNotify(registryUrl, path, providerUrls));
            }
        });
    }

    /**
     * Unsubscribe the listener from specified consumer url
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    protected void doUnsubscribe(Url consumerUrl, ProviderDiscoveryListener listener) {
        ConcurrentHashSet<ProviderDiscoveryListener> listeners = path2Listeners.get(consumerUrl.getPath());
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                path2Listeners.remove(consumerUrl.getPath());
            }
        }

        // Unsubscribe service listener
        unsubscribeListener(consumerUrl, listener);
        log.info("Unsubscribed the listener for the consumer url [{}]", consumerUrl);
    }

    protected abstract void doRegister(Url url);

    protected abstract void doDeregister(Url url);

    protected abstract void doActivate(Url url);

    protected abstract void doDeactivate(Url url);

    protected abstract List<Url> discoverProviders(Url consumerUrl);

    protected abstract void subscribeListener(Url consumerUrl, ProviderDiscoveryListener listener);

    protected abstract void unsubscribeListener(Url consumerUrl, ProviderDiscoveryListener listener);
}
