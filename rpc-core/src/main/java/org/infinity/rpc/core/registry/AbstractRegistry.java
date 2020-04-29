package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.registry.listener.NotifyListener;
import org.infinity.rpc.core.switcher.SwitcherUtils;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractRegistry implements Registry {
    private Map<Url, Map<String, List<Url>>> subscribedCategoryResponses = new ConcurrentHashMap<>();

    private   Url      registryUrl;
    private   Set<Url> registeredServiceUrls = new ConcurrentHashSet<Url>();
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
    public void register(Url url) {
        if (url == null) {
            log.error("Failed to register a null url with the [{}]", registryClassName);
            return;
        }
        Url copy = removeUnnecessaryParams(url.copy());
        doRegister(copy);
        log.info("Registered the url [{}] on registry [{}] by using [{}]", url, registryUrl.getIdentity(), registryClassName);
        registeredServiceUrls.add(url);
        // available if heartbeat switcher already open
        if (SwitcherUtils.isOpen(SwitcherUtils.REGISTRY_HEARTBEAT_SWITCHER)) {
            available(url);
        }
    }

    @Override
    public void unregister(Url Url) {
        if (Url == null) {
            log.warn("[{}] unregister with malformed param, Url is null", registryClassName);
            return;
        }
        log.info("[{}] Url ({}) will unregister to Registry [{}]", registryClassName, Url, registryUrl.getIdentity());
        doUnregister(removeUnnecessaryParams(Url.copy()));
        registeredServiceUrls.remove(Url);
    }

    @Override
    public void subscribe(Url Url, NotifyListener listener) {
        if (Url == null || listener == null) {
            log.warn("[{}] subscribe with malformed param, Url:{}, listener:{}", registryClassName, Url, listener);
            return;
        }
        log.info("[{}] Listener ({}) will subscribe to Url ({}) in Registry [{}]", registryClassName, listener, Url,
                registryUrl.getIdentity());
        doSubscribe(Url.copy(), listener);
    }

    @Override
    public void unsubscribe(Url Url, NotifyListener listener) {
        if (Url == null || listener == null) {
            log.warn("[{}] unsubscribe with malformed param, Url:{}, listener:{}", registryClassName, Url, listener);
            return;
        }
        log.info("[{}] Listener ({}) will unsubscribe from Url ({}) in Registry [{}]", registryClassName, listener, Url,
                registryUrl.getIdentity());
        doUnsubscribe(Url.copy(), listener);
    }

    @Override
    public List<Url> discover(Url Url) {
        if (Url == null) {
            log.warn("[{}] discover with malformed param, refUrl is null", registryClassName);
            return Collections.EMPTY_LIST;
        }
        Url = Url.copy();
        List<Url> results = new ArrayList<Url>();

        Map<String, List<Url>> categoryUrls = subscribedCategoryResponses.get(Url);
        if (categoryUrls != null && categoryUrls.size() > 0) {
            for (List<Url> Urls : categoryUrls.values()) {
                for (Url tempUrl : Urls) {
                    results.add(tempUrl.copy());
                }
            }
        } else {
            List<Url> UrlsDiscovered = doDiscover(Url);
            if (UrlsDiscovered != null) {
                for (Url u : UrlsDiscovered) {
                    results.add(u.copy());
                }
            }
        }
        return results;
    }

    @Override
    public Url getUrl() {
        return registryUrl;
    }

    @Override
    public Collection<Url> getRegisteredServiceUrls() {
        return registeredServiceUrls;
    }

    @Override
    public void available(Url Url) {
        log.info("[{}] Url ({}) will set to available to Registry [{}]", registryClassName, Url, registryUrl.getIdentity());
        if (Url != null) {
            doAvailable(removeUnnecessaryParams(Url.copy()));
        } else {
            doAvailable(null);
        }
    }

    @Override
    public void unavailable(Url Url) {
        log.info("[{}] Url ({}) will set to unavailable to Registry [{}]", registryClassName, Url, registryUrl.getIdentity());
        if (Url != null) {
            doUnavailable(removeUnnecessaryParams(Url.copy()));
        } else {
            doUnavailable(null);
        }
    }

    protected List<Url> getCachedUrls(Url Url) {
        Map<String, List<Url>> rsUrls = subscribedCategoryResponses.get(Url);
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

    protected void notify(Url refUrl, NotifyListener listener, List<Url> Urls) {
        if (listener == null || Urls == null) {
            return;
        }
        Map<String, List<Url>> nodeTypeUrlsInRs = new HashMap<String, List<Url>>();
        for (Url sUrl : Urls) {
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
            listener.notify(getUrl(), us);
        }
    }

    /**
     * 移除不必提交到注册中心的参数。这些参数不需要被client端感知。
     *
     * @param Url
     */
    private Url removeUnnecessaryParams(Url Url) {
        // codec参数不能提交到注册中心，如果client端没有对应的codec会导致client端不能正常请求。
        Url.getParameters().remove(UrlParam.codec.getName());
        return Url;
    }

    protected abstract void doRegister(Url Url);

    protected abstract void doUnregister(Url Url);

    protected abstract void doSubscribe(Url url, NotifyListener listener);

    protected abstract void doUnsubscribe(Url url, NotifyListener listener);

    protected abstract List<Url> doDiscover(Url url);

    protected abstract void doAvailable(Url Url);

    protected abstract void doUnavailable(Url Url);
}
