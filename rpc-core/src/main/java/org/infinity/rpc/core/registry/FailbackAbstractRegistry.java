package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;
import org.infinity.rpc.utilities.destory.ShutdownHook;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The registry can automatically recover services when encountered the failure.
 */
@Slf4j
public abstract class FailbackAbstractRegistry extends AbstractRegistry {

    private Set<Url>                                    failedRegisteredUrl              = new ConcurrentHashSet<>();
    private Set<Url>                                    failedUnregisteredUrl            = new ConcurrentHashSet<>();
    private Map<Url, ConcurrentHashSet<ClientListener>> failedSubscriptionPerClientUrl   = new ConcurrentHashMap<>();
    private Map<Url, ConcurrentHashSet<ClientListener>> failedUnsubscriptionPerClientUrl = new ConcurrentHashMap<>();

    /**
     * A retry single thread pool can reconnect registry periodically
     */
    private static ScheduledExecutorService retryThreadPool = Executors.newScheduledThreadPool(1);

    static {
        ShutdownHook.add(() -> {
            if (!retryThreadPool.isShutdown()) {
                retryThreadPool.shutdown();
            }
        });
    }

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
        long retryInterval = registryUrl.getIntParameter(Url.PARAM_RETRY_INTERVAL);
        // Retry to connect registry at retry interval
        retryThreadPool.scheduleAtFixedRate(() -> {
            // Do retry task
            doRetry();
        }, retryInterval, retryInterval, TimeUnit.MILLISECONDS);
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
     * @param providerUrl url
     */
    @Override
    public void register(Url providerUrl) {
        failedRegisteredUrl.remove(providerUrl);
        failedUnregisteredUrl.remove(providerUrl);

        try {
            super.register(providerUrl);
        } catch (Exception e) {
            // In extreme cases, it can cause register failure
            if (isCheckingUrls(getRegistryUrl(), providerUrl)) {
                throw new RuntimeException(MessageFormat.format("Failed to register the url [{0}] to registry [{1}] by using [{2}]", providerUrl, getRegistryUrl(), getRegistryClassName()), e);
            }
            failedRegisteredUrl.add(providerUrl);
        }
    }

    /**
     * Unregister the url from registry
     *
     * @param providerUrl url
     */
    @Override
    public void unregister(Url providerUrl) {
        failedRegisteredUrl.remove(providerUrl);
        failedUnregisteredUrl.remove(providerUrl);

        try {
            super.unregister(providerUrl);
        } catch (Exception e) {
            // In extreme cases, it can cause register failure
            if (isCheckingUrls(getRegistryUrl(), providerUrl)) {
                throw new RuntimeException(MessageFormat.format("Failed to unregister the url [{0}] to registry [{1}] by using [{2}]", providerUrl, getRegistryUrl(), getRegistryClassName()), e);
            }
            failedUnregisteredUrl.add(providerUrl);
        }
    }

    /**
     * It contains the functionality of method subscribeServiceListener and subscribeCommandListener
     *
     * @param clientUrl client url
     * @param listener  notify listener
     */
    @Override
    public void subscribe(Url clientUrl, ClientListener listener) {
        // Remove failed listener from the set before subscribe
        removeFailedListener(clientUrl, listener);

        try {
            super.subscribe(clientUrl, listener);
        } catch (Exception e) {
            // Add the failed listener to the set if exception occurred
            List<Url> cachedUrls = super.getCachedProviderUrls(clientUrl);
            if (CollectionUtils.isNotEmpty(cachedUrls)) {
                listener.onSubscribe(getRegistryUrl(), cachedUrls);
            } else if (isCheckingUrls(getRegistryUrl(), clientUrl)) {
                log.warn(String.format("[%s] false to subscribe %s from %s", getRegistryClassName(), clientUrl, getRegistryUrl()), e);
                throw new RuntimeException(String.format("[%s] false to subscribe %s from %s", getRegistryClassName(), clientUrl, getRegistryUrl()), e);
            }
            addToFailedMap(failedSubscriptionPerClientUrl, clientUrl, listener);
        }
    }

    @Override
    public void unsubscribe(Url clientUrl, ClientListener listener) {
        removeFailedListener(clientUrl, listener);

        try {
            super.unsubscribe(clientUrl, listener);
        } catch (Exception e) {
            if (isCheckingUrls(getRegistryUrl(), clientUrl)) {
                throw new RuntimeException(String.format("[%s] false to unsubscribe %s from %s", getRegistryClassName(), clientUrl, getRegistryUrl()),
                        e);
            }
            addToFailedMap(failedUnsubscriptionPerClientUrl, clientUrl, listener);
        }
    }

    private boolean isCheckingUrls(Url... urls) {
        for (Url url : urls) {
            if (!Boolean.parseBoolean(url.getParameter(Url.PARAM_CHECK_HEALTH))) {
                return false;
            }
        }
        return true;
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

    /**
     * Get all the provider urls based on the client url
     *
     * @param clientUrl client url
     * @return provider urls
     */
    @Override
    public List<Url> discover(Url clientUrl) {
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
}