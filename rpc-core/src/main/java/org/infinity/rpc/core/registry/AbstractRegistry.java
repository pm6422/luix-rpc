package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.registry.listener.NotifyListener;
import org.infinity.rpc.core.switcher.SwitcherUtils;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract registry
 */
@Slf4j
public abstract class AbstractRegistry implements Registry {
    private Map<Url, Map<String, List<Url>>> subscribedCategoryResponses = new ConcurrentHashMap<>();

    private   Url      registryUrl;
    private   Set<Url> registeredServiceUrls = new ConcurrentHashSet<>();
    protected String   registryClassName     = this.getClass().getSimpleName();

    public AbstractRegistry(Url Url) {
        this.registryUrl = Url.copy();
        // register a heartbeat switcher to perceive service state change and change available state
        SwitcherUtils.initSwitcher(SwitcherUtils.REGISTRY_HEARTBEAT_SWITCHER, false);
        SwitcherUtils.registerSwitcherListener(SwitcherUtils.REGISTRY_HEARTBEAT_SWITCHER, (key, value) -> {
            if (key != null && value != null) {
                if (value) {
                    available(null);
                } else {
                    unavailable(null);
                }
            }
        });
    }

    @Override
    public Url getRegistryUrl() {
        return registryUrl;
    }

    @Override
    public Collection<Url> getRegisteredServiceUrls() {
        return registeredServiceUrls;
    }

    /**
     * Register url
     *
     * @param url url
     */
    @Override
    public void register(Url url) {
        if (url == null) {
            log.error("Failed to register a null url with the [{}]", registryClassName);
            return;
        }
        Url copy = removeUnnecessaryParams(url.copy());
        doRegister(copy);
        log.info("Registered the url [{}] to registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
        // Added it to the container after registered
        registeredServiceUrls.add(url);
        // available if heartbeat switcher already open
        if (SwitcherUtils.isOpen(SwitcherUtils.REGISTRY_HEARTBEAT_SWITCHER)) {
            available(url);
        }
    }

    @Override
    public void unregister(Url url) {
        if (url == null) {
            log.warn("[{}] unregister with malformed param, Url is null", registryClassName);
            return;
        }
        log.info("Unregistered the url [{}] from registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
        doUnregister(removeUnnecessaryParams(url.copy()));
        // Removed it from the container after unregistered
        registeredServiceUrls.remove(url);
    }

    @Override
    public void subscribe(Url url, NotifyListener listener) {
        if (url == null || listener == null) {
            log.warn("[{}] subscribe with malformed param, Url:{}, listener:{}", registryClassName, url, listener);
            return;
        }
        log.info("[{}] Listener ({}) will subscribe to Url ({}) in Registry [{}]", registryClassName, listener, url,
                registryUrl.getIdentity());
        doSubscribe(url.copy(), listener);
    }

    @Override
    public void unsubscribe(Url url, NotifyListener listener) {
        if (url == null || listener == null) {
            log.warn("[{}] unsubscribe with malformed param, Url:{}, listener:{}", registryClassName, url, listener);
            return;
        }
        log.info("[{}] Listener ({}) will unsubscribe from Url ({}) in Registry [{}]", registryClassName, listener, url,
                registryUrl.getIdentity());
        doUnsubscribe(url.copy(), listener);
    }

    @Override
    public List<Url> discover(Url url) {
        if (url == null) {
            log.warn("[{}] discover with malformed param, refUrl is null", registryClassName);
            return Collections.EMPTY_LIST;
        }
        url = url.copy();
        List<Url> results = new ArrayList<Url>();

        Map<String, List<Url>> categoryUrls = subscribedCategoryResponses.get(url);
        if (categoryUrls != null && categoryUrls.size() > 0) {
            for (List<Url> Urls : categoryUrls.values()) {
                for (Url tempUrl : Urls) {
                    results.add(tempUrl.copy());
                }
            }
        } else {
            List<Url> UrlsDiscovered = doDiscover(url);
            if (UrlsDiscovered != null) {
                for (Url u : UrlsDiscovered) {
                    results.add(u.copy());
                }
            }
        }
        return results;
    }

    @Override
    public void available(Url url) {
        log.info("[{}] Url ({}) will set to available to Registry [{}]", registryClassName, url, registryUrl.getIdentity());
        if (url != null) {
            doAvailable(removeUnnecessaryParams(url.copy()));
        } else {
            doAvailable(null);
        }
    }

    @Override
    public void unavailable(Url url) {
        log.info("[{}] Url ({}) will set to unavailable to Registry [{}]", registryClassName, url, registryUrl.getIdentity());
        if (url != null) {
            doUnavailable(removeUnnecessaryParams(url.copy()));
        } else {
            doUnavailable(null);
        }
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
        Map<String, List<Url>> nodeTypeUrlsInRs = new HashMap<String, List<Url>>();
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
            listener.notify(getRegistryUrl(), us);
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

    protected abstract void doSubscribe(Url url, NotifyListener listener);

    protected abstract void doUnsubscribe(Url url, NotifyListener listener);

    protected abstract List<Url> doDiscover(Url url);

    protected abstract void doAvailable(Url url);

    protected abstract void doUnavailable(Url url);
}
