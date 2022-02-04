package org.infinity.luix.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.luix.core.exception.impl.RpcConfigException;
import org.infinity.luix.core.listener.client.ConsumerListener;
import org.infinity.luix.core.listener.server.ProviderListener;
import org.infinity.luix.core.listener.server.impl.CommandProviderListener;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.annotation.EventPublisher;
import org.infinity.luix.utilities.collection.ConcurrentHashSet;
import org.infinity.luix.utilities.concurrent.NotThreadSafe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.infinity.luix.core.constant.ProtocolConstants.CODEC;

/**
 * Abstract registry
 */
@Slf4j
@NotThreadSafe
public abstract class AbstractRegistry implements Registry {
    /**
     * The registry subclass name
     */
    private final String                            registryClassName                    = this.getClass().getSimpleName();
    /**
     * Registry url
     */
    protected     Url                               registryUrl;
    /**
     * Registered provider urls cache
     */
    private final Set<Url>                          registeredProviderUrls               = new ConcurrentHashSet<>();
    /**
     * Registered consumer urls cache
     */
    private final Set<Url>                          registeredConsumerUrls               = new ConcurrentHashSet<>();
    /**
     * Provider urls cache grouped by 'type' parameter value of {@link Url}
     */
    private final Map<Url, Map<String, List<Url>>>  providerUrlsPerTypePerConsumerUrl    = new ConcurrentHashMap<>();
    private final Map<Url, CommandProviderListener> commandServiceListenerPerConsumerUrl = new ConcurrentHashMap<>();

    /**
     * Get registry instance class name
     *
     * @return registry instance class name
     */
    @Override
    public String getRegistryClassName() {
        return registryClassName;
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

    public Map<Url, CommandProviderListener> getCommandServiceListenerPerConsumerUrl() {
        return commandServiceListenerPerConsumerUrl;
    }

    public AbstractRegistry(Url registryUrl) {
        Validate.notNull(registryUrl, "Registry url must NOT be null!");
        this.registryUrl = registryUrl;
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
        log.info("Deregistered the url [{}] from registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
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
            log.info("Activated the url [{}] on registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
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
            log.info("Deactivated the url [{}] on registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
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
     * Subscribe the consumer url to specified listener
     *
     * @param consumerUrl consumer url
     * @param listener    listener
     */
    @Override
    public void subscribe(Url consumerUrl, ConsumerListener listener) {
        Validate.notNull(consumerUrl, "Consumer url must NOT be null!");
        Validate.notNull(listener, "Consumer listener must NOT be null!");

        doSubscribe(consumerUrl, listener);
        log.info("Subscribed the url [{}] to listener [{}] by using [{}]", registryUrl.getIdentity(), listener, registryClassName);
    }

    /**
     * Unsubscribe the url from specified listener
     *
     * @param consumerUrl provider url
     * @param listener    listener
     */
    @Override
    public void unsubscribe(Url consumerUrl, ConsumerListener listener) {
        Validate.notNull(consumerUrl, "Consumer url must NOT be null!");
        Validate.notNull(listener, "Consumer listener must NOT be null!");

        doUnsubscribe(consumerUrl, listener);
        log.info("Unsubscribed the url [{}] from listener [{}] by using [{}]", registryUrl.getIdentity(), listener, registryClassName);
    }

    /**
     * todo: check usage
     * Get all the provider urls based on the consumer url
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Url> discover(Url consumerUrl) {
        if (consumerUrl == null) {
            log.warn("Url must NOT be null!");
            return Collections.EMPTY_LIST;
        }
        List<Url> results = new ArrayList<>();
        Map<String, List<Url>> urlsPerType = providerUrlsPerTypePerConsumerUrl.get(consumerUrl);
        if (MapUtils.isNotEmpty(urlsPerType)) {
            // Get all the provider urls from cache no matter what the type is
            results = urlsPerType.values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } else {
            // Read the provider urls from registry if local cache does not exist
            List<Url> discoveredUrls = doDiscover(consumerUrl);
            if (CollectionUtils.isNotEmpty(discoveredUrls)) {
                // Make a url copy and add to results
                results = discoveredUrls.stream().map(Url::copy).collect(Collectors.toList());
            }
        }
        return results;
    }

    /**
     * Get provider urls from cache
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    protected List<Url> getCachedProviderUrls(Url consumerUrl) {
        Map<String, List<Url>> urls = providerUrlsPerTypePerConsumerUrl.get(consumerUrl);
        if (MapUtils.isEmpty(urls)) {
            return Collections.emptyList();
        }
        return urls.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Group urls by url type parameter, and put it in local cache, then execute the listener
     *
     * @param providerUrls provider urls
     * @param consumerUrl  consumer url
     * @param listener     listener
     */
    protected void notify(List<Url> providerUrls, Url consumerUrl, ConsumerListener listener) {
        if (listener == null || CollectionUtils.isEmpty(providerUrls)) {
            return;
        }
        // Group urls by type parameter value of url
        Map<String, List<Url>> providerUrlsPerType = groupUrlsByType(providerUrls);

        Map<String, List<Url>> cachedProviderUrlsPerType = providerUrlsPerTypePerConsumerUrl.get(consumerUrl);
        if (cachedProviderUrlsPerType == null) {
            cachedProviderUrlsPerType = new ConcurrentHashMap<>();
            providerUrlsPerTypePerConsumerUrl.putIfAbsent(consumerUrl, cachedProviderUrlsPerType);
        }

        // Update urls cache
        cachedProviderUrlsPerType.putAll(providerUrlsPerType);

        for (Map.Entry<String, List<Url>> entry : providerUrlsPerType.entrySet()) {
            @EventPublisher("providersDiscoveryEvent")
            List<Url> providerUrlList = entry.getValue();
            listener.onNotify(registryUrl, consumerUrl, providerUrlList);
        }
    }

    /**
     * Group urls by type parameter value of url
     *
     * @param urls urls
     * @return grouped url per url parameter type
     */
    private Map<String, List<Url>> groupUrlsByType(List<Url> urls) {
        Map<String, List<Url>> urlsPerType = new HashMap<>();
        for (Url url : urls) {
            String type = url.getOption(Url.PARAM_TYPE, Url.PARAM_TYPE_PROVIDER);
            List<Url> urlList = urlsPerType.computeIfAbsent(type, k -> new ArrayList<>());
            urlList.add(url);
        }
        return urlsPerType;
    }

    protected abstract void doRegister(Url url);

    protected abstract void doDeregister(Url url);

    protected abstract void doActivate(Url url);

    protected abstract void doDeactivate(Url url);

    public abstract List<Url> discoverActiveProviders(Url consumerUrl);

    /**
     * It contains the functionality of method subscribeServiceListener and subscribeCommandListener
     * And execute the listener
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    protected void doSubscribe(Url consumerUrl, ConsumerListener listener) {
        Url consumerUrlCopy = consumerUrl.copy();
        // Create a new command service listener or get it from cache
        CommandProviderListener commandServiceListener = getCommandServiceListener(consumerUrlCopy);
        // Add client listener to command service listener, and use command service listener to manage listener
        commandServiceListener.addNotifyListener(listener);

        // Trigger onNotify method of commandServiceListener if child change event happens
        subscribeProviderListener(consumerUrlCopy, commandServiceListener);

        // Discover active providers
        List<Url> providerUrls = doDiscover(consumerUrlCopy);
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            // Notify discovered providers to client side
            this.notify(providerUrls, consumerUrlCopy, listener);
        }
        log.info("Subscribed the listener for the url [{}]", consumerUrl);
    }

    /**
     * Get or put command service listener from or to cache
     *
     * @param consumerUrl consumer url
     * @return command service listener
     */
    private CommandProviderListener getCommandServiceListener(Url consumerUrl) {
        CommandProviderListener listener = commandServiceListenerPerConsumerUrl.get(consumerUrl);
        if (listener == null) {
            // Pass the specified registry instance to CommandServiceListener, e.g, ZookeeperRegistry
            listener = new CommandProviderListener(consumerUrl, this);
            CommandProviderListener commandServiceListener = commandServiceListenerPerConsumerUrl.putIfAbsent(consumerUrl, listener);
            if (commandServiceListener != null) {
                // Key exists in map, return old data
                listener = commandServiceListener;
            }
        }
        return listener;
    }

    /**
     * Unsubscribe the service and command listener
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    protected void doUnsubscribe(Url consumerUrl, ConsumerListener listener) {
        Url urlCopy = consumerUrl.copy();
        CommandProviderListener commandServiceListener = commandServiceListenerPerConsumerUrl.get(urlCopy);
        // Remove notify listener from command service listener
        commandServiceListener.removeNotifyListener(listener);
        // Unsubscribe service listener
        unsubscribeProviderListener(urlCopy, commandServiceListener);
        log.info("Unsubscribed the listener for the url [{}]", consumerUrl);
    }

    protected abstract void subscribeProviderListener(Url consumerUrl, ProviderListener listener);

    protected abstract void unsubscribeProviderListener(Url consumerUrl, ProviderListener listener);

    /**
     * Discover the provider or command url
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    protected List<Url> doDiscover(Url consumerUrl) {
        Url urlCopy = consumerUrl.copy();
        List<Url> providerUrls = discoverActiveProviders(urlCopy);
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            log.info("Discovered the provider urls [{}] for url [{}]", providerUrls, consumerUrl);
        } else {
            log.warn("No RPC service providers found on registry for consumer url [{}]!", consumerUrl);
        }
        return providerUrls;
    }
}
