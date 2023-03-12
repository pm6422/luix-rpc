package com.luixtech.rpc.core.registry;

import com.luixtech.rpc.core.constant.RegistryConstants;
import com.luixtech.rpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.rpc.core.listener.ProviderDiscoveryListener;
import com.luixtech.rpc.core.thread.ScheduledThreadPool;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.utilities.lang.collection.ConcurrentHashSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The registry can automatically recover services when encountered the failure.
 */
@Slf4j
public abstract class FailbackAbstractRegistry extends AbstractRegistry {

    /**
     * Registration failure provider or consumer urls.
     */
    private final Set<Url>                                               registerFailedUrls                     = new ConcurrentHashSet<>();
    /**
     * De-registration failure provider or consumer urls.
     */
    private final Set<Url>                                               deregisterFailedUrls                   = new ConcurrentHashSet<>();
    /**
     * Key: consumer url.
     * Value: subscription failure listeners.
     */
    private final Map<Url, ConcurrentHashSet<ProviderDiscoveryListener>> consumerUrl2SubscribeFailedListeners   = new ConcurrentHashMap<>();
    /**
     * Key: consumer url.
     * Value: unsubscription failure listeners.
     */
    private final Map<Url, ConcurrentHashSet<ProviderDiscoveryListener>> consumerUrl2UnsubscribeFailedListeners = new ConcurrentHashMap<>();

    public FailbackAbstractRegistry(Url registryUrl) {
        super(registryUrl);
        scheduleRetry(registryUrl);
    }

    /**
     * Register the url to registry
     *
     * @param url provider or consumer url
     */
    @Override
    public void register(Url url) {
        Validate.notNull(url, "Url must NOT be null!");

        try {
            super.register(url);
            registerFailedUrls.remove(url);
        } catch (Exception e) {
            // In some extreme cases, it can cause register failure
            throw new RpcFrameworkException(MessageFormat.format("Failed to register [{0}] to registry [{1}] by using [{2}]",
                    url, registryUrl, this.getClass().getSimpleName()), e);
        }
    }

    /**
     * Deregister the url from registry
     *
     * @param url provider or consumer url
     */
    @Override
    public void deregister(Url url) {
        Validate.notNull(url, "Url must NOT be null!");

        try {
            super.deregister(url);
            deregisterFailedUrls.remove(url);
        } catch (Exception e) {
            // In extreme cases, it can cause register failure
            throw new RpcFrameworkException(
                    MessageFormat.format("Failed to deregister [{0}] from registry [{1}] by using [{2}]",
                            url, registryUrl, this.getClass().getSimpleName()), e);
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
    public void subscribe(Url consumerUrl, ProviderDiscoveryListener listener) {
        Validate.notNull(consumerUrl, "Consumer url must NOT be null!");
        Validate.notNull(listener, "Listener must NOT be null!");

        // Remove failed listener from the local cache before subscribe
        removeFailedListener(consumerUrl, listener);
        try {
            super.subscribe(consumerUrl, listener);
        } catch (Exception e) {
            // Add the failed listener to the local cache if exception occurred in order to retry later
            List<Url> cachedProviderUrls = super.discoverProviders(consumerUrl, true);
            if (CollectionUtils.isNotEmpty(cachedProviderUrls)) {
                // Notify if the cached provider urls not empty
                listener.onNotify(registryUrl, consumerUrl.getPath(), cachedProviderUrls);
            }
            Optional.ofNullable(globalProviderDiscoveryListener).ifPresent(l ->
                    l.onNotify(registryUrl, consumerUrl.getPath(), cachedProviderUrls));

            consumerUrl2SubscribeFailedListeners.computeIfAbsent(consumerUrl, k -> new ConcurrentHashSet<>()).add(listener);
            throw new RpcFrameworkException(
                    MessageFormat.format("Failed to subscribe the listener [{0}] to the client [{1}] " +
                                    "on registry [{2}] by using [{3}]",
                            listener, consumerUrl, registryUrl, this.getClass().getSimpleName()), e);
        }
    }

    /**
     * It contains the functionality of method unsubscribeServiceListener and unsubscribeCommandListener
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    @Override
    public void unsubscribe(Url consumerUrl, ProviderDiscoveryListener listener) {
        Validate.notNull(consumerUrl, "Consumer url must NOT be null!");
        Validate.notNull(listener, "Listener must NOT be null!");

        removeFailedListener(consumerUrl, listener);
        try {
            super.unsubscribe(consumerUrl, listener);
        } catch (Exception e) {
            consumerUrl2UnsubscribeFailedListeners.computeIfAbsent(consumerUrl, k -> new ConcurrentHashSet<>()).add(listener);
            throw new RpcFrameworkException(
                    MessageFormat.format("Failed to unsubscribe the listener [{0}] from the client [{1}] " +
                                    "on registry [{2}] by using [{3}]",
                            listener, consumerUrl, registryUrl, this.getClass().getSimpleName()), e);
        }
    }

    /**
     * Schedule the retry registration and subscription process periodically
     *
     * @param registryUrl registry url
     */
    private void scheduleRetry(Url registryUrl) {
        long retryInterval = registryUrl.getIntOption(RegistryConstants.RETRY_INTERVAL, RegistryConstants.RETRY_INTERVAL_VAL_DEFAULT);
        // Retry to connect registry at retry interval
        ScheduledThreadPool.schedulePeriodicalTask(ScheduledThreadPool.RETRY_THREAD_POOL, retryInterval, () -> {
            doRetryFailedRegistration();
            doRetryFailedDeregistration();
            doRetryFailedSubscription();
            doRetryFailedUnsubscription();
        });
    }

    private void doRetryFailedRegistration() {
        if (CollectionUtils.isEmpty(registerFailedUrls)) {
            return;
        }
        Iterator<Url> iterator = registerFailedUrls.iterator();
        while (iterator.hasNext()) {
            Url url = iterator.next();
            try {
                super.register(url);
                iterator.remove();
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to retry to register [{0}] by [{1}] and it will retry later!",
                        url, this.getClass().getSimpleName()), e);
            }
        }
        log.info("Retried to register failed urls by {}", this.getClass().getSimpleName());
    }

    private void doRetryFailedDeregistration() {
        if (CollectionUtils.isEmpty(deregisterFailedUrls)) {
            return;
        }
        Iterator<Url> iterator = deregisterFailedUrls.iterator();
        while (iterator.hasNext()) {
            Url url = iterator.next();
            try {
                super.deregister(url);
                iterator.remove();
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to retry to deregister [{0}] by [{1}] and it will retry later!",
                        url, this.getClass().getSimpleName()), e);
            }
        }
        log.info("Retried to deregister failed urls by {}", this.getClass().getSimpleName());
    }

    private void doRetryFailedSubscription() {
        if (MapUtils.isEmpty(consumerUrl2SubscribeFailedListeners)) {
            return;
        }
        // Do the clean empty value task
        for (Map.Entry<Url, ConcurrentHashSet<ProviderDiscoveryListener>> entry : consumerUrl2SubscribeFailedListeners.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                consumerUrl2SubscribeFailedListeners.remove(entry.getKey());
            }
        }
        if (MapUtils.isEmpty(consumerUrl2SubscribeFailedListeners)) {
            return;
        }
        for (Map.Entry<Url, ConcurrentHashSet<ProviderDiscoveryListener>> entry : consumerUrl2SubscribeFailedListeners.entrySet()) {
            Url url = entry.getKey();
            Iterator<ProviderDiscoveryListener> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                ProviderDiscoveryListener listener = iterator.next();
                try {
                    super.subscribe(url, listener);
                    iterator.remove();
                } catch (Exception e) {
                    log.warn(MessageFormat.format("Failed to retry to subscribe listener [{0}] to url [{1}] by [{2}] " +
                            "and it will retry later!", listener.getClass().getSimpleName(), url, this.getClass().getSimpleName()), e);
                }
            }
        }
        log.info("Retried to subscribe listener by {}", this.getClass().getSimpleName());
    }

    private void doRetryFailedUnsubscription() {
        if (MapUtils.isEmpty(consumerUrl2UnsubscribeFailedListeners)) {
            return;
        }
        // Do the clean empty value task
        for (Map.Entry<Url, ConcurrentHashSet<ProviderDiscoveryListener>> entry : consumerUrl2UnsubscribeFailedListeners.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                consumerUrl2UnsubscribeFailedListeners.remove(entry.getKey());
            }
        }
        if (MapUtils.isEmpty(consumerUrl2UnsubscribeFailedListeners)) {
            return;
        }
        for (Map.Entry<Url, ConcurrentHashSet<ProviderDiscoveryListener>> entry : consumerUrl2UnsubscribeFailedListeners.entrySet()) {
            Url url = entry.getKey();
            Iterator<ProviderDiscoveryListener> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                ProviderDiscoveryListener listener = iterator.next();
                try {
                    super.unsubscribe(url, listener);
                    iterator.remove();
                } catch (Exception e) {
                    log.warn(MessageFormat.format("Failed to retry to unsubscribe listener [{0}] to url [{1}] by [{2}] " +
                            "and it will retry later!", listener.getClass().getSimpleName(), url, this.getClass().getSimpleName()), e);
                }
            }
        }
        log.info("Retried to unsubscribe listener by {}", this.getClass().getSimpleName());
    }

    private void removeFailedListener(Url consumerUrl, ProviderDiscoveryListener listener) {
        Set<ProviderDiscoveryListener> listeners = consumerUrl2SubscribeFailedListeners.get(consumerUrl);
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.remove(listener);
        }
        listeners = consumerUrl2UnsubscribeFailedListeners.get(consumerUrl);
        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.remove(listener);
        }
    }
}