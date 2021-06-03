package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.thread.ScheduledThreadPool;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.constant.RegistryConstants.RETRY_INTERVAL;
import static org.infinity.rpc.core.constant.RegistryConstants.RETRY_INTERVAL_VAL_DEFAULT;

/**
 * The registry can automatically recover services when encountered the failure.
 */
@Slf4j
public abstract class FailbackAbstractRegistry extends AbstractRegistry {

    private final Set<Url>                                    failedRegisteredUrl                = new ConcurrentHashSet<>();
    private final Set<Url>                                    failedUnregisteredUrl              = new ConcurrentHashSet<>();
    private final Map<Url, ConcurrentHashSet<ClientListener>> failedSubscriptionPerConsumerUrl   = new ConcurrentHashMap<>();
    private final Map<Url, ConcurrentHashSet<ClientListener>> failedUnsubscriptionPerConsumerUrl = new ConcurrentHashMap<>();

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
        long retryInterval = registryUrl.getIntOption(RETRY_INTERVAL, RETRY_INTERVAL_VAL_DEFAULT);
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
        if (MapUtils.isEmpty(failedSubscriptionPerConsumerUrl)) {
            return;
        }
        // Do the clean empty value task
        for (Map.Entry<Url, ConcurrentHashSet<ClientListener>> entry : failedSubscriptionPerConsumerUrl.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                failedSubscriptionPerConsumerUrl.remove(entry.getKey());
            }
        }
        if (MapUtils.isEmpty(failedSubscriptionPerConsumerUrl)) {
            return;
        }
        for (Map.Entry<Url, ConcurrentHashSet<ClientListener>> entry : failedSubscriptionPerConsumerUrl.entrySet()) {
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
        if (MapUtils.isEmpty(failedUnsubscriptionPerConsumerUrl)) {
            return;
        }
        // Do the clean empty value task
        for (Map.Entry<Url, ConcurrentHashSet<ClientListener>> entry : failedUnsubscriptionPerConsumerUrl.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                failedUnsubscriptionPerConsumerUrl.remove(entry.getKey());
            }
        }
        if (MapUtils.isEmpty(failedUnsubscriptionPerConsumerUrl)) {
            return;
        }
        for (Map.Entry<Url, ConcurrentHashSet<ClientListener>> entry : failedUnsubscriptionPerConsumerUrl.entrySet()) {
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
            failedRegisteredUrl.add(providerUrl);
            throw new RpcFrameworkException(MessageFormat.format("Failed to register provider [{0}] to registry [{1}] by using [{2}]",
                    providerUrl, registryUrl, getRegistryClassName()), e);
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
            failedUnregisteredUrl.add(providerUrl);
            throw new RpcFrameworkException(MessageFormat.format("Failed to unregister provider [{0}] from registry [{1}] by using [{2}]",
                    providerUrl, registryUrl, getRegistryClassName()), e);
        }
    }

    /**
     * It contains the functionality of method subscribeServiceListener and subscribeCommandListener
     * And execute the listener
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    @Override
    public void subscribe(Url consumerUrl, ClientListener listener) {
        Validate.notNull(consumerUrl, "Client url must NOT be null!");
        Validate.notNull(listener, "Client listener must NOT be null!");

        // Remove failed listener from the local cache before subscribe
        removeFailedListener(consumerUrl, listener);

        try {
            super.subscribe(consumerUrl, listener);
        } catch (Exception e) {
            log.warn("Exception occurred!", e);
            // Add the failed listener to the local cache if exception occurred in order to retry later
            List<Url> cachedProviderUrls = super.getCachedProviderUrls(consumerUrl);
            if (CollectionUtils.isNotEmpty(cachedProviderUrls)) {
                // Notify if the cached provider urls not empty
                listener.onNotify(registryUrl, cachedProviderUrls);
            }
            addToFailedMap(failedSubscriptionPerConsumerUrl, consumerUrl, listener);
            throw new RpcFrameworkException(MessageFormat.format("Failed to subscribe the listener [{0}] to the client [{1}] on registry [{2}] by using [{3}]",
                    listener, consumerUrl, registryUrl, getRegistryClassName()), e);
        }
    }

    /**
     * It contains the functionality of method unsubscribeServiceListener and unsubscribeCommandListener
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    @Override
    public void unsubscribe(Url consumerUrl, ClientListener listener) {
        Validate.notNull(consumerUrl, "Client url must NOT be null!");
        Validate.notNull(listener, "Client listener must NOT be null!");

        removeFailedListener(consumerUrl, listener);

        try {
            super.unsubscribe(consumerUrl, listener);
        } catch (Exception e) {
            addToFailedMap(failedUnsubscriptionPerConsumerUrl, consumerUrl, listener);
            throw new RpcFrameworkException(MessageFormat.format("Failed to unsubscribe the listener [{0}] from the client [{1}] on registry [{2}] by using [{3}]",
                    listener, consumerUrl, registryUrl, getRegistryClassName()), e);
        }
    }

    /**
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
        try {
            return super.discover(consumerUrl);
        } catch (Exception e) {
            log.warn(MessageFormat.format("Failed to discover provider urls with consumer url {0} on registry [{1}]!", consumerUrl, registryUrl), e);
            return Collections.EMPTY_LIST;
        }
    }

    private void addToFailedMap(Map<Url, ConcurrentHashSet<ClientListener>> failedMap, Url consumerUrl, ClientListener listener) {
        Set<ClientListener> listeners = failedMap.get(consumerUrl);
        if (listeners == null) {
            failedMap.putIfAbsent(consumerUrl, new ConcurrentHashSet<>());
            listeners = failedMap.get(consumerUrl);
        }
        listeners.add(listener);
    }

    private void removeFailedListener(Url consumerUrl, ClientListener listener) {
        Set<ClientListener> listeners = failedSubscriptionPerConsumerUrl.get(consumerUrl);
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.remove(listener);
        }
        listeners = failedUnsubscriptionPerConsumerUrl.get(consumerUrl);
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.remove(listener);
        }
    }
}