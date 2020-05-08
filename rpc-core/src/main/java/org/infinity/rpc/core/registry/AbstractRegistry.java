package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.registry.listener.NotifyListener;
import org.infinity.rpc.core.switcher.DefaultSwitcherService;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.switcher.SwitcherService.REGISTRY_HEARTBEAT_SWITCHER;

/**
 * Abstract registry
 */
@Slf4j
public abstract class AbstractRegistry implements Registry {
    /**
     * The subclass name
     */
    protected String                           registryClassName           = this.getClass().getSimpleName();
    /**
     * Registry url
     */
    private   Url                              registryUrl;
    /**
     * Registered provider urls
     */
    private   Set<Url>                         registeredProviderUrls      = new ConcurrentHashSet<>();
    /**
     *
     */
    private   Map<Url, Map<String, List<Url>>> subscribedCategoryResponses = new ConcurrentHashMap<>();

    @Override
    public Url getRegistryUrl() {
        return registryUrl;
    }

    @Override
    public Collection<Url> getRegisteredProviderUrls() {
        return registeredProviderUrls;
    }


    public AbstractRegistry(Url Url) {
        this.registryUrl = Url.copy();
        registerSwitcherListener();
    }

    /**
     * Register a heartbeat switcher to perceive provider state change
     */
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
     * Register url to registry
     *
     * @param url url
     */
    @Override
    public void register(Url url) {
        if (url == null) {
            log.warn("Url must NOT be null!");
            return;
        }
        doRegister(removeUnnecessaryParams(url.copy()));
        log.info("Registered the url [{}] to registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
        // Added it to the container after registered
        registeredProviderUrls.add(url);
        // Move the url to active node of registry if heartbeat switcher already open
        if (DefaultSwitcherService.getInstance().isOn(REGISTRY_HEARTBEAT_SWITCHER)) {
            activate(url);
        }
    }

    /**
     * Unregister url from registry
     *
     * @param url url
     */
    @Override
    public void unregister(Url url) {
        if (url == null) {
            log.warn("Url must NOT be null!");
            return;
        }
        doUnregister(removeUnnecessaryParams(url.copy()));
        log.info("Unregistered the url [{}] from registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
        // Removed it from the container after unregistered
        registeredProviderUrls.remove(url);
    }

    /**
     * Register the url to 'active' node of registry
     *
     * @param url url
     */
    @Override
    public void activate(Url url) {
        if (url != null) {
            doActivate(removeUnnecessaryParams(url.copy()));
        } else {
            doActivate(null);
        }
        log.info("Activated the url [{}] on registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
    }

    /**
     * Register the url to 'inactive' node of registry
     *
     * @param url url
     */
    @Override
    public void deactivate(Url url) {
        if (url != null) {
            doDeactivate(removeUnnecessaryParams(url.copy()));
        } else {
            doDeactivate(null);
        }
        log.info("Deactivated the url [{}] on registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
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
     * Subscribe the url to specified listener
     *
     * @param url      url
     * @param listener listener
     */
    @Override
    public void subscribe(Url url, NotifyListener listener) {
        if (url == null) {
            log.warn("Url must NOT be null!");
            return;
        }
        if (listener == null) {
            log.warn("Listener must NOT be null!");
            return;
        }
        // TODO: url copy mechanism
        doSubscribe(url, listener);
        log.info("Subscribed the url [{}] to listener [{}] by using [{}]", registryUrl.getIdentity(), listener, registryClassName);
    }

    /**
     * Unsubscribe the url from specified listener
     *
     * @param url      url
     * @param listener listener
     */
    @Override
    public void unsubscribe(Url url, NotifyListener listener) {
        if (url == null) {
            log.warn("Url must NOT be null!");
            return;
        }
        if (listener == null) {
            log.warn("Listener must NOT be null!");
            return;
        }
        // TODO: url copy mechanism
        doUnsubscribe(url, listener);
        log.info("Unsubscribed the url [{}] from listener [{}] by using [{}]", registryUrl.getIdentity(), listener, registryClassName);
    }

    @Override
    public List<Url> discover(Url url) {
        if (url == null) {
            log.warn("Url must NOT be null!");
            return Collections.EMPTY_LIST;
        }
        Url copy = url.copy();
        List<Url> results = new ArrayList<>();

        Map<String, List<Url>> categoryUrls = subscribedCategoryResponses.get(copy);
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

    protected List<Url> getCachedUrls(Url url) {
        Map<String, List<Url>> rsUrls = subscribedCategoryResponses.get(url);
        if (rsUrls == null || rsUrls.size() == 0) {
            return null;
        }

        List<Url> Urls = new ArrayList<>();
        for (List<Url> us : rsUrls.values()) {
            for (Url tempUrl : us) {
                Urls.add(tempUrl.copy());
            }
        }
        return Urls;
    }

    protected void notify(Url refUrl, NotifyListener listener, List<Url> urls) {
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
        Map<String, List<Url>> cUrls = subscribedCategoryResponses.get(refUrl);
        if (cUrls == null) {
            subscribedCategoryResponses.putIfAbsent(refUrl, new ConcurrentHashMap<String, List<Url>>());
            cUrls = subscribedCategoryResponses.get(refUrl);
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
