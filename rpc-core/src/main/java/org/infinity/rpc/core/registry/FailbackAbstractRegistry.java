package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.destroy.ScheduledThreadPool;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.constant.RegistryConstants.RETRY_INTERVAL;
import static org.infinity.rpc.core.constant.RegistryConstants.RETRY_INTERVAL_DEFAULT_VALUE;
import static org.infinity.rpc.core.constant.ServiceConstants.CHECK_HEALTH;

/**
 * The registry can automatically recover services when encountered the failure.
 */
@Slf4j
public abstract class FailbackAbstractRegistry extends AbstractRegistry {

    private final Set<Url>                                    failedRegisteredUrl              = new ConcurrentHashSet<>();
    private final Set<Url>                                    failedUnregisteredUrl            = new ConcurrentHashSet<>();
    private final Map<Url, ConcurrentHashSet<ClientListener>> failedSubscriptionPerClientUrl   = new ConcurrentHashMap<>();
    private final Map<Url, ConcurrentHashSet<ClientListener>> failedUnsubscriptionPerClientUrl = new ConcurrentHashMap<>();

    public FailbackAbstractRegistry(Url registryUrl) {
        super(registryUrl);
        scheduleRetry(registryUrl);
    }

    /**
     * Schedule the retry registration and subscription process periodically
     *
     * @param registryUrl registry url
     */
    private void scheduleRetry(Url registryUrl) {
        long retryInterval = registryUrl.getIntOption(RETRY_INTERVAL, RETRY_INTERVAL_DEFAULT_VALUE);
        // Retry to connect registry at retry interval
        ScheduledThreadPool.schedulePeriodicalTask(ScheduledThreadPool.RETRY_THREAD_POOL, retryInterval, this::doRetry);
    }

    /**
     * Retry registration for failed record
     */
    private void doRetry() {
        doRetryFailedRegistration();
        doRetryFailedUnregistration();
        doRetryFailedSubscription();
        doRetryFailedUnsubscription();
    }

    private void doRetryFailedRegistration() {
        if (CollectionUtils.isEmpty(failedRegisteredUrl)) {
            return;
        }
        Iterator<Url> iterator = failedRegisteredUrl.iterator();
        while (iterator.hasNext()) {
            Url url = iterator.next();
            try {
                super.register(url);
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to retry to register [{0}] by [{1}] and it will be retry later!", url, getRegistryClassName()), e);
            }
            iterator.remove();
        }
        log.info("Retried to register urls by {}", getRegistryClassName());
    }

    private void doRetryFailedUnregistration() {
        if (CollectionUtils.isEmpty(failedUnregisteredUrl)) {
            return;
        }
        Iterator<Url> iterator = failedUnregisteredUrl.iterator();
        while (iterator.hasNext()) {
            Url url = iterator.next();
            try {
                super.unregister(url);
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to retry to unregister [{0}] by [{1}] and it will be retry later!", url, getRegistryClassName()), e);
            }
            iterator.remove();
        }
        log.info("Retried to unregister urls by {}", getRegistryClassName());
    }

    private void doRetryFailedSubscription() {
        if (MapUtils.isEmpty(failedSubscriptionPerClientUrl)) {
            return;
        }
        // Do the clean empty value task
        for (Map.Entry<Url, ConcurrentHashSet<ClientListener>> entry : failedSubscriptionPerClientUrl.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                failedSubscriptionPerClientUrl.remove(entry.getKey());
            }
        }
        if (MapUtils.isEmpty(failedSubscriptionPerClientUrl)) {
            return;
        }
        for (Map.Entry<Url, ConcurrentHashSet<ClientListener>> entry : failedSubscriptionPerClientUrl.entrySet()) {
            Url url = entry.getKey();
            Iterator<ClientListener> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                ClientListener listener = iterator.next();
                try {
                    super.subscribe(url, listener);
                } catch (Exception e) {
                    log.warn(MessageFormat.format("Failed to retry to subscribe listener [{0}] to url [{1}] by [{2}] " +
                            "and it will be retry later!", listener.getClass().getSimpleName(), url, getRegistryClassName()), e);
                }
                iterator.remove();
            }
        }
        log.info("Retried to subscribe listener to urls by {}", getRegistryClassName());
    }

    private void doRetryFailedUnsubscription() {
        if (MapUtils.isEmpty(failedUnsubscriptionPerClientUrl)) {
            return;
        }
        // Do the clean empty value task
        for (Map.Entry<Url, ConcurrentHashSet<ClientListener>> entry : failedUnsubscriptionPerClientUrl.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                failedUnsubscriptionPerClientUrl.remove(entry.getKey());
            }
        }
        if (MapUtils.isEmpty(failedUnsubscriptionPerClientUrl)) {
            return;
        }
        for (Map.Entry<Url, ConcurrentHashSet<ClientListener>> entry : failedUnsubscriptionPerClientUrl.entrySet()) {
            Url url = entry.getKey();
            Iterator<ClientListener> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                ClientListener listener = iterator.next();
                try {
                    super.unsubscribe(url, listener);
                } catch (Exception e) {
                    log.warn(MessageFormat.format("Failed to retry to unsubscribe listener [{0}] to url [{1}] by [{2}] " +
                            "and it will be retry later!", listener.getClass().getSimpleName(), url, getRegistryClassName()), e);
                }
                iterator.remove();
            }
        }
        log.info("Retried to unsubscribe listener to urls by {}", getRegistryClassName());
    }

    /**
     * Register the url to registry
     *
     * @param providerUrl provider url
     */
    @Override
    public void register(Url providerUrl) {
        Validate.notNull(providerUrl, "Provider url must NOT be null!");
        failedRegisteredUrl.remove(providerUrl);
        failedUnregisteredUrl.remove(providerUrl);

        try {
            super.register(providerUrl);
        } catch (Exception e) {
            // In some extreme cases, it can cause register failure
            if (forceCheckHealth(getRegistryUrl(), providerUrl)) {
                throw new RuntimeException(MessageFormat.format("Failed to register provider [{0}] to registry [{1}] by using [{2}]",
                        providerUrl, getRegistryUrl(), getRegistryClassName()), e);
            }
            failedRegisteredUrl.add(providerUrl);
        }
    }

    /**
     * Unregister the url from registry
     *
     * @param providerUrl provider url
     */
    @Override
    public void unregister(Url providerUrl) {
        Validate.notNull(providerUrl, "Provider url must NOT be null!");
        failedRegisteredUrl.remove(providerUrl);
        failedUnregisteredUrl.remove(providerUrl);

        try {
            super.unregister(providerUrl);
        } catch (Exception e) {
            // In extreme cases, it can cause register failure
            if (forceCheckHealth(getRegistryUrl(), providerUrl)) {
                throw new RuntimeException(MessageFormat.format("Failed to unregister provider [{0}] from registry [{1}] by using [{2}]",
                        providerUrl, getRegistryUrl(), getRegistryClassName()), e);
            }
            failedUnregisteredUrl.add(providerUrl);
        }
    }

    /**
     * It contains the functionality of method subscribeServiceListener and subscribeCommandListener
     * And execute the listener
     *
     * @param clientUrl client url
     * @param listener  client listener
     */
    @Override
    public void subscribe(Url clientUrl, ClientListener listener) {
        Validate.notNull(clientUrl, "Client url must NOT be null!");
        Validate.notNull(listener, "Client listener must NOT be null!");

        // Remove failed listener from the local cache before subscribe
        removeFailedListener(clientUrl, listener);

        try {
            super.subscribe(clientUrl, listener);
        } catch (Exception e) {
            log.warn("Exception occurred!", e);
            // Add the failed listener to the local cache if exception occurred in order to retry later
            List<Url> cachedProviderUrls = super.getCachedProviderUrls(clientUrl);
            if (CollectionUtils.isNotEmpty(cachedProviderUrls)) {
                // Notify if the cached provider urls not empty
                listener.onNotify(getRegistryUrl(), cachedProviderUrls);
            } else if (forceCheckHealth(getRegistryUrl(), clientUrl)) {
                throw new RuntimeException(MessageFormat.format("Failed to subscribe the listener [{0}] to the client [{1}] on registry [{2}] by using [{3}]",
                        listener, clientUrl, getRegistryUrl(), getRegistryClassName()), e);
            }
            addToFailedMap(failedSubscriptionPerClientUrl, clientUrl, listener);
        }
    }

    /**
     * It contains the functionality of method unsubscribeServiceListener and unsubscribeCommandListener
     *
     * @param clientUrl client url
     * @param listener  client listener
     */
    @Override
    public void unsubscribe(Url clientUrl, ClientListener listener) {
        Validate.notNull(clientUrl, "Client url must NOT be null!");
        Validate.notNull(listener, "Client listener must NOT be null!");

        removeFailedListener(clientUrl, listener);

        try {
            super.unsubscribe(clientUrl, listener);
        } catch (Exception e) {
            if (forceCheckHealth(getRegistryUrl(), clientUrl)) {
                throw new RuntimeException(MessageFormat.format("Failed to unsubscribe the listener [{0}] from the client [{1}] on registry [{2}] by using [{3}]",
                        listener, clientUrl, getRegistryUrl(), getRegistryClassName()), e);
            }
            addToFailedMap(failedUnsubscriptionPerClientUrl, clientUrl, listener);
        }
    }

    /**
     * Check whether all the urls contain the PARAM_CHECK_HEALTH of {@link Url}
     *
     * @param urls urls
     * @return true: all the urls contain PARAM_CHECK_HEALTH, false: any url does not contain PARAM_CHECK_HEALTH
     */
    private boolean forceCheckHealth(Url... urls) {
        return Arrays.stream(urls).allMatch(url -> url.getBooleanOption(CHECK_HEALTH));
    }

    /**
     * Get all the provider urls based on the client url
     *
     * @param clientUrl client url
     * @return provider urls
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<Url> discover(Url clientUrl) {
        if (clientUrl == null) {
            log.warn("Url must NOT be null!");
            return Collections.EMPTY_LIST;
        }
        try {
            return super.discover(clientUrl);
        } catch (Exception e) {
            log.warn(MessageFormat.format("Failed to discover provider urls with client url {0} on registry [{1}]!", clientUrl, getRegistryUrl()), e);
            return Collections.EMPTY_LIST;
        }
    }

    private void addToFailedMap(Map<Url, ConcurrentHashSet<ClientListener>> failedMap, Url clientUrl, ClientListener listener) {
        Set<ClientListener> listeners = failedMap.get(clientUrl);
        if (listeners == null) {
            failedMap.putIfAbsent(clientUrl, new ConcurrentHashSet<>());
            listeners = failedMap.get(clientUrl);
        }
        listeners.add(listener);
    }

    private void removeFailedListener(Url clientUrl, ClientListener listener) {
        Set<ClientListener> listeners = failedSubscriptionPerClientUrl.get(clientUrl);
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.remove(listener);
        }
        listeners = failedUnsubscriptionPerClientUrl.get(clientUrl);
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.remove(listener);
        }
    }
}