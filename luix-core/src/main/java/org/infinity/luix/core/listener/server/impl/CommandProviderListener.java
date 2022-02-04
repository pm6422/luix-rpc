package org.infinity.luix.core.listener.server.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.listener.client.ConsumerListener;
import org.infinity.luix.core.listener.server.ProviderListener;
import org.infinity.luix.core.registry.AbstractRegistry;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.utilities.annotation.EventReceiver;
import org.infinity.luix.utilities.collection.ConcurrentHashSet;
import org.infinity.luix.utilities.concurrent.NotThreadSafe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * todo: CommandServiceManager
 * One command service listener for one consumer or provider interface class.
 */
@Slf4j
@NotThreadSafe
public class CommandProviderListener implements ProviderListener {

    /**
     * Consumer url
     */
    private final Url                    consumerUrl;
    /**
     * Registry
     */
    private final AbstractRegistry       registry;
    /**
     *
     */
    private final Set<ConsumerListener>  consumerListeners         = new ConcurrentHashSet<>();
    /**
     * Active provider urls per form map
     */
    private final Map<String, List<Url>> activeProviderUrlsPerForm = new ConcurrentHashMap<>();

    public CommandProviderListener(Url consumerUrl, AbstractRegistry registry) {
        this.consumerUrl = consumerUrl;
        this.registry = registry;
        log.info("Created listener for url [{}]", consumerUrl.toFullStr());
    }

    /**
     * Add notify listener to container
     *
     * @param clientListener notify listener to be added
     */
    public void addNotifyListener(ConsumerListener clientListener) {
        consumerListeners.add(clientListener);
    }

    /**
     * Remove notify listener from container
     *
     * @param clientListener notify listener to be removed
     */
    public void removeNotifyListener(ConsumerListener clientListener) {
        consumerListeners.remove(clientListener);
    }

    public Set<ConsumerListener> getConsumerListeners() {
        return consumerListeners;
    }

    /**
     * Service listener event
     *
     * @param registryUrl  registry url
     * @param consumerUrl  consumer url
     * @param providerUrls provider urls
     */
    @EventReceiver("providersChangeEvent")
    @Override
    public void onNotify(Url registryUrl, Url consumerUrl, List<Url> providerUrls) {
//        log.info("Receive providers change event, consumerUrl [{}], registryUrl [{}], providerUrls [{}]",
//                consumerUrl.toFullStr(), registryUrl.toFullStr(), providerUrls);
        String form = consumerUrl.getForm();
        activeProviderUrlsPerForm.put(form, providerUrls);

        List<Url> providerUrlList;
//            log.info("Discovering the active provider urls based on group param of url when RPC command is null");
        providerUrlList = new ArrayList<>(discoverActiveProvidersByGroup(this.consumerUrl));

        for (ConsumerListener consumerListener : consumerListeners) {
            consumerListener.onNotify(registry.getRegistryUrl(), consumerUrl, providerUrlList);
            log.debug("Invoked event: {}", consumerListener);
        }
    }


    /**
     * Discover providers urls based on group param of url
     *
     * @param consumerUrl consumer url
     * @return active provider urls
     */
    private List<Url> discoverActiveProvidersByGroup(Url consumerUrl) {
        String group = consumerUrl.getForm();
        List<Url> providerUrls = activeProviderUrlsPerForm.get(group);
        if (providerUrls == null) {
            providerUrls = registry.discoverActiveProviders(consumerUrl);
            activeProviderUrlsPerForm.put(group, providerUrls);
        }
//        log.info("Discovered url by param group of url [{}]", consumerUrl);
        return providerUrls;
    }


    @Override
    public String toString() {
        return CommandProviderListener.class.getSimpleName().concat(":").concat(consumerUrl.getPath());
    }
}
