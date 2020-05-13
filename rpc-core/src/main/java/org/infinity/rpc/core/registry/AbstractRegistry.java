package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.registry.listener.NotifyListener;
import org.infinity.rpc.core.switcher.DefaultSwitcherService;
import org.infinity.rpc.utilities.annotation.Event;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.infinity.rpc.core.switcher.SwitcherService.REGISTRY_HEARTBEAT_SWITCHER;

/**
 * Abstract registry
 */
@Slf4j
public abstract class AbstractRegistry implements Registry {
    /**
     * The subclass name
     */
    protected String                           registryClassName                       = this.getClass().getSimpleName();
    /**
     * Registry url
     */
    private   Url                              registryUrl;
    /**
     * Registered provider urls
     */
    private   Set<Url>                         registeredProviderUrls                  = new ConcurrentHashSet<>();
    /**
     *
     */
    private   Map<Url, Map<String, List<Url>>> subscribedCategoryResponsesPerClientUrl = new ConcurrentHashMap<>();

    @Override
    public Url getRegistryUrl() {
        return registryUrl;
    }

    @Override
    public Collection<Url> getRegisteredProviderUrls() {
        return registeredProviderUrls;
    }


    public AbstractRegistry(Url registryUrl) {
        this.registryUrl = registryUrl;
        registerSwitcherListener();
    }

    /**
     * Register a heartbeat switcher to perceive provider state change
     */
    @Event
    private void registerSwitcherListener() {
        DefaultSwitcherService.getInstance().initSwitcher(REGISTRY_HEARTBEAT_SWITCHER, false);

        // Register anonymous inner class of AbstractRegistry as a listener
        DefaultSwitcherService.getInstance().registerListener(REGISTRY_HEARTBEAT_SWITCHER, (name, switchOn) -> {
            if (StringUtils.isNotEmpty(name) && switchOn != null) {
                if (switchOn) {
                    // switch on
                    activate(null);
                } else {
                    // switch off
                    deactivate(null);
                }
            }
        });
    }

    /**
     * Register provider url to registry
     *
     * @param providerUrl provider url
     */
    @Override
    public void register(Url providerUrl) {
        if (providerUrl == null) {
            log.warn("Url must NOT be null!");
            return;
        }
        doRegister(removeUnnecessaryParams(providerUrl.copy()));
        log.info("Registered the url [{}] to registry [{}] by using [{}]", providerUrl, registryUrl.getIdentity(), registryClassName);
        // Added it to the container after registered
        registeredProviderUrls.add(providerUrl);
        // Move the url to active node of registry if heartbeat switcher already open
        if (DefaultSwitcherService.getInstance().isOn(REGISTRY_HEARTBEAT_SWITCHER)) {
            activate(providerUrl);
        }
    }

    /**
     * Unregister provider url from registry
     *
     * @param providerUrl provider url
     */
    @Override
    public void unregister(Url providerUrl) {
        if (providerUrl == null) {
            log.warn("Url must NOT be null!");
            return;
        }
        doUnregister(removeUnnecessaryParams(providerUrl.copy()));
        log.info("Unregistered the url [{}] from registry [{}] by using [{}]", providerUrl, registryUrl.getIdentity(), registryClassName);
        // Removed it from the container after unregistered
        registeredProviderUrls.remove(providerUrl);
    }

    /**
     * Register the url to 'active' node of registry
     *
     * @param providerUrl provider url
     */
    @Override
    public void activate(Url providerUrl) {
        if (providerUrl != null) {
            doActivate(removeUnnecessaryParams(providerUrl.copy()));
        } else {
            doActivate(null);
        }
        log.info("Activated the url [{}] on registry [{}] by using [{}]", providerUrl, registryUrl.getIdentity(), registryClassName);
    }

    /**
     * Register the url to 'inactive' node of registry
     *
     * @param providerUrl provider url
     */
    @Override
    public void deactivate(Url providerUrl) {
        if (providerUrl != null) {
            doDeactivate(removeUnnecessaryParams(providerUrl.copy()));
        } else {
            doDeactivate(null);
        }
        log.info("Deactivated the url [{}] on registry [{}] by using [{}]", providerUrl, registryUrl.getIdentity(), registryClassName);
    }

    /**
     * Remove the unnecessary url param to register to registry in order to not to be seen by consumer
     *
     * @param url url
     */
    private Url removeUnnecessaryParams(Url url) {
        // codec parameter can not be registered to registry,
        // because client side may could not request successfully if client side does not have the codec.
        url.getParameters().remove(Url.PARAM_CODEC);
        return url;
    }

    /**
     * Subscribe the url to specified listener todo: modify comments
     *
     * @param clientUrl client url
     * @param listener  listener
     */
    @Override
    public void subscribe(Url clientUrl, NotifyListener listener) {
        if (clientUrl == null) {
            log.warn("Url must NOT be null!");
            return;
        }
        if (listener == null) {
            log.warn("Listener must NOT be null!");
            return;
        }
        // TODO: url copy mechanism
        doSubscribe(clientUrl, listener);
        log.info("Subscribed the url [{}] to listener [{}] by using [{}]", registryUrl.getIdentity(), listener, registryClassName);
    }

    /**
     * Unsubscribe the url from specified listener
     *
     * @param clientUrl provider url
     * @param listener  listener
     */
    @Override
    public void unsubscribe(Url clientUrl, NotifyListener listener) {
        if (clientUrl == null) {
            log.warn("Url must NOT be null!");
            return;
        }
        if (listener == null) {
            log.warn("Listener must NOT be null!");
            return;
        }
        // TODO: url copy mechanism
        doUnsubscribe(clientUrl, listener);
        log.info("Unsubscribed the url [{}] from listener [{}] by using [{}]", registryUrl.getIdentity(), listener, registryClassName);
    }

    @Override
    public List<Url> discover(Url clientUrl) {
        if (clientUrl == null) {
            log.warn("Url must NOT be null!");
            return Collections.EMPTY_LIST;
        }
        Url copy = clientUrl.copy();
        List<Url> results = new ArrayList<>();

        Map<String, List<Url>> categoryUrls = subscribedCategoryResponsesPerClientUrl.get(copy);
        if (MapUtils.isNotEmpty(categoryUrls)) {
            for (List<Url> Urls : categoryUrls.values()) {
                for (Url tempUrl : Urls) {
                    results.add(tempUrl.copy());
                }
            }
        } else {
            List<Url> discoveredUrls = doDiscover(copy);
            if (CollectionUtils.isNotEmpty(discoveredUrls)) {
                for (Url u : discoveredUrls) {
                    results.add(u.copy());
                }
            }
        }
        return results;
    }

    protected List<Url> getCachedUrls(Url clientUrl) {
        Map<String, List<Url>> urls = subscribedCategoryResponsesPerClientUrl.get(clientUrl);
        if (MapUtils.isEmpty(urls)) {
            return Collections.emptyList();
        }
        List<Url> results = urls.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return results;
    }

    protected void notify(Url clientUrl, NotifyListener listener, List<Url> urls) {
        if (listener == null || urls == null) {
            return;
        }
        Map<String, List<Url>> nodeTypeUrlsInRs = new HashMap<>();
        for (Url sUrl : urls) {
            String nodeType = sUrl.getParameter(UrlParam.nodeType.getName(), UrlParam.nodeType.getValue());
            List<Url> oneNodeTypeUrls = nodeTypeUrlsInRs.get(nodeType);
            if (oneNodeTypeUrls == null) {
                nodeTypeUrlsInRs.put(nodeType, new ArrayList<Url>());
                oneNodeTypeUrls = nodeTypeUrlsInRs.get(nodeType);
            }
            oneNodeTypeUrls.add(sUrl);
        }
        Map<String, List<Url>> cUrls = subscribedCategoryResponsesPerClientUrl.get(clientUrl);
        if (cUrls == null) {
            subscribedCategoryResponsesPerClientUrl.putIfAbsent(clientUrl, new ConcurrentHashMap<>());
            cUrls = subscribedCategoryResponsesPerClientUrl.get(clientUrl);
        }

        // refresh local Urls cache
        cUrls.putAll(nodeTypeUrlsInRs);

        for (List<Url> us : nodeTypeUrlsInRs.values()) {
            listener.onSubscribe(getRegistryUrl(), us);
        }
    }

    protected abstract void doRegister(Url url);

    protected abstract void doUnregister(Url url);

    protected abstract void doActivate(Url url);

    protected abstract void doDeactivate(Url url);

    protected abstract void doSubscribe(Url url, NotifyListener listener);

    protected abstract void doUnsubscribe(Url url, NotifyListener listener);

    protected abstract List<Url> doDiscover(Url url);
}
