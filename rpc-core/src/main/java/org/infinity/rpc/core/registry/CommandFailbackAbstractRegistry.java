package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.NotifyListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class CommandFailbackAbstractRegistry extends FailbackAbstractRegistry {
    private Map<Url, CommandServiceListener> commandServiceListenerCacheMap = new ConcurrentHashMap<>();

    public CommandFailbackAbstractRegistry(Url url) {
        super(url);
        log.info("CommandFailbackRegistry init. url: " + url.toSimpleString());
    }

    /**
     * It contains the functionality of method subscribeServiceListener and subscribeCommandListener
     *
     * @param url
     * @param listener
     */
    protected void doSubscribe(Url url, final NotifyListener listener) {
        Url urlCopy = url.copy();
        CommandServiceListener commandServiceListener = getCommandServiceListener(urlCopy);
        commandServiceListener.addNotifyListener(listener);

        // Child change event
        subscribeServiceListener(urlCopy, commandServiceListener);
        // Data change event
        subscribeCommandListener(urlCopy, commandServiceListener);

        List<Url> urls = doDiscover(urlCopy);
        if (CollectionUtils.isNotEmpty(urls)) {
            this.notify(urlCopy, listener, urls);
        }
        log.info("Subscribed the listener for the url [{}]", url);
    }

    protected void doUnsubscribe(Url url, NotifyListener listener) {
        log.info("CommandFailbackRegistry unsubscribe. url: " + url.toSimpleString());
        Url urlCopy = url.copy();
        CommandServiceListener manager = commandServiceListenerCacheMap.get(urlCopy);

        manager.removeNotifyListener(listener);
        unsubscribeServiceListener(urlCopy, manager);
        unsubscribeCommandListener(urlCopy, manager);
    }

    /**
     * Discover the provider url or command
     *
     * @param url url
     * @return
     */
    protected List<Url> doDiscover(Url url) {
        List<Url> urls;

        Url urlCopy = url.copy();
        String commandStr = discoverCommand(urlCopy);
        RpcCommand rpcCommand = null;
        if (StringUtils.isNotEmpty(commandStr)) {
            rpcCommand = RpcCommandUtils.stringToCommand(commandStr);
        }

        if (rpcCommand != null) {
            rpcCommand.sort();
            CommandServiceListener commandServiceListener = getCommandServiceListener(urlCopy);
            urls = commandServiceListener.discoverServiceWithCommand(urlCopy, new HashMap<>(), rpcCommand);
            // 在subscribeCommon时，可能订阅完马上就notify，导致首次notify指令时，可能还有其他service没有完成订阅，
            // 此处先对manager更新指令，避免首次订阅无效的问题。
            commandServiceListener.setCommandCache(commandStr);
            log.info("Discovered the command [{}] for url [{}]", commandStr, url);
        } else {
            urls = discoverProviders(urlCopy);
            log.info("Discovered the provider urls [{}] for url [{}]", urls, url);
        }
        return urls;
    }

    public List<Url> commandPreview(Url url, RpcCommand rpcCommand, String previewIP) {
        List<Url> finalResult;
        Url urlCopy = url.copy();

        if (rpcCommand != null) {
            CommandServiceListener manager = getCommandServiceListener(urlCopy);
            finalResult = manager.discoverServiceWithCommand(urlCopy, new HashMap<String, Integer>(), rpcCommand, previewIP);
        } else {
            finalResult = discoverProviders(urlCopy);
        }

        return finalResult;
    }

    /**
     * Get or put command service listener from or to cache
     *
     * @param urlCopy urk
     * @return command service listener
     */
    private CommandServiceListener getCommandServiceListener(Url urlCopy) {
        CommandServiceListener manager = commandServiceListenerCacheMap.get(urlCopy);
        if (manager == null) {
            // Pass the specified registry instance to CommandServiceManager, e.g, ZookeeperRegistry
            manager = new CommandServiceListener(urlCopy, this);
            CommandServiceListener serviceManager = commandServiceListenerCacheMap.putIfAbsent(urlCopy, manager);
            if (serviceManager != null) {
                // Key exists in map, return old data
                manager = serviceManager;
            }
        }
        return manager;
    }

    /**
     * Get command service listener cache map
     *
     * @return command service listener cache map
     */
    public Map<Url, CommandServiceListener> getCommandServiceListenerCacheMap() {
        return commandServiceListenerCacheMap;
    }

    protected abstract void subscribeServiceListener(Url url, ServiceListener listener);

    protected abstract void unsubscribeServiceListener(Url url, ServiceListener listener);

    protected abstract void subscribeCommandListener(Url url, CommandListener listener);

    protected abstract void unsubscribeCommandListener(Url url, CommandListener listener);

    protected abstract List<Url> discoverProviders(Url url);

    protected abstract String discoverCommand(Url url);
}
