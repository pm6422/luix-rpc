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
    protected String   registryClassName      = this.getClass().getSimpleName();
    /**
     * Registry url
     */
    private   Url      registryUrl;
    /**
     * Registered provider urls
     */
    private   Set<Url> registeredProviderUrls = new ConcurrentHashSet<>();

    private Map<Url, Map<String, List<Url>>> subscribedCategoryResponses = new ConcurrentHashMap<>();


    public AbstractRegistry(Url Url) {
        this.registryUrl = Url.copy();
        registerSwitcherListener();
    }

    /**
     * Register a heartbeat switcher to perceive service state change
     */
    private void registerSwitcherListener() {
        DefaultSwitcherService.getInstance().initSwitcher(REGISTRY_HEARTBEAT_SWITCHER, false);

        // Register anonymous inner class of AbstractRegistry as listener
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

    @Override
    public Url getRegistryUrl() {
        return registryUrl;
    }

    @Override
    public Collection<Url> getRegisteredProviderUrls() {
        return registeredProviderUrls;
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
        Url copy = removeUnnecessaryParams(url.copy());
        doRegister(copy);
        log.info("Registered the url [{}] to registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
        // Added it to the container after registered
        registeredProviderUrls.add(url);
        // available if heartbeat switcher already open
        if (DefaultSwitcherService.getInstance().isOn(REGISTRY_HEARTBEAT_SWITCHER)) {
            activate(url);
        }
    }

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

    @Override
    public void activate(Url url) {
        log.info("Activate the url [{}] on registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
        if (url != null) {
            doActivate(removeUnnecessaryParams(url.copy()));
        } else {
            doActivate(null);
        }
    }

    @Override
    public void deactivate(Url url) {
        log.info("Deactivate the url [{}] on registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
        if (url != null) {
            doDeactivate(removeUnnecessaryParams(url.copy()));
        } else {
            doDeactivate(null);
        }
    }

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
        doSubscribe(url.copy(), listener);
        log.info("Subscribed the url [{}] to listener [{}] by using [{}]", registryUrl.getIdentity(), listener, registryClassName);
    }

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
        doUnsubscribe(url.copy(), listener);
        log.info("Unsubscribed the url [{}] from listener [{}] by using [{}]", registryUrl.getIdentity(), listener, registryClassName);
    }

    @Override
    public List<Url> discover(Url url) {
        if (url == null) {
            log.warn("Url must NOT be null!");
            return Collections.EMPTY_LIST;
        }
        url = url.copy();
        List<Url> results = new ArrayList<>();

        Map<String, List<Url>> categoryUrls = subscribedCategoryResponses.get(url);
        if (MapUtils.isNotEmpty(categoryUrls)) {
            for (List<Url> Urls : categoryUrls.values()) {
                for (Url tempUrl : Urls) {
                    results.add(tempUrl.copy());
                }
            }
        } else {
            List<Url> discoveredUrls = doDiscover(url);
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

        List<Url> Urls = new ArrayList<Url>();
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

    /**
     * 移除不必提交到注册中心的参数。这些参数不需要被client端感知。
     *
     * @param url
     */
    private Url removeUnnecessaryParams(Url url) {
        // codec参数不能提交到注册中心，如果client端没有对应的codec会导致client端不能正常请求。
        url.getParameters().remove(UrlParam.codec.getName());
        return url;
    }

    protected abstract void doRegister(Url url);

    protected abstract void doUnregister(Url url);

    protected abstract void doActivate(Url url);

    protected abstract void doDeactivate(Url url);

    protected abstract void doSubscribe(Url url, NotifyListener listener);

    protected abstract void doUnsubscribe(Url url, NotifyListener listener);

    protected abstract List<Url> doDiscover(Url url);
}
