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
    }

    public Map<Url, CommandServiceListener> getCommandServiceListenerCacheMap() {
        return commandServiceListenerCacheMap;
    }

    /**
     * It contains the functionality of method subscribeServiceListener and subscribeCommandListener
     *
     * @param url      client url
     * @param listener notify listener
     */
    protected void doSubscribe(Url url, final NotifyListener listener) {
        Url urlCopy = url.copy();
        // Get or put command service listener from or to cache
        CommandServiceListener commandServiceListener = getCommandServiceListener(urlCopy);
        // Add notify listener to command service listener
        commandServiceListener.addNotifyListener(listener);

        // Trigger onSubscribe method of commandServiceListener if child change event happens
        subscribeServiceListener(urlCopy, commandServiceListener);
        // Trigger onSubscribe method of commandServiceListener if data change event happens
        subscribeCommandListener(urlCopy, commandServiceListener);

        List<Url> urls = doDiscover(urlCopy);
        if (CollectionUtils.isNotEmpty(urls)) {
            this.notify(urlCopy, listener, urls);
        }
        log.info("Subscribed the listener for the url [{}]", url);
    }

    /**
     * Unsubscribe the service and command listener
     *
     * @param url      url
     * @param listener notify listener
     */
    protected void doUnsubscribe(Url url, NotifyListener listener) {
        Url urlCopy = url.copy();
        CommandServiceListener commandServiceListener = commandServiceListenerCacheMap.get(urlCopy);
        // Remove notify listener from command service listener
        commandServiceListener.removeNotifyListener(listener);
        // Unsubscribe service listener
        unsubscribeServiceListener(urlCopy, commandServiceListener);
        // Unsubscribe command listener
        unsubscribeCommandListener(urlCopy, commandServiceListener);
        log.info("Unsubscribed the listener for the url [{}]", url);
    }

    /**
     * Discover the provider or command url
     *
     * @param url url
     * @return
     */
    protected List<Url> doDiscover(Url url) {
        List<Url> urls;

        Url urlCopy = url.copy();
        // Read command json content of specified url
        String commandStr = readCommand(urlCopy);
        RpcCommand rpcCommand = null;
        if (StringUtils.isNotEmpty(commandStr)) {
            rpcCommand = RpcCommandUtils.convertToCommand(commandStr);
        }

        if (rpcCommand != null) {
            rpcCommand.sort();
            CommandServiceListener commandServiceListener = getCommandServiceListener(urlCopy);
            urls = commandServiceListener.discoverServiceWithCommand(urlCopy, new HashMap<>(), rpcCommand);
            // 在subscribeCommon时，可能订阅完马上就notify，导致首次notify指令时，可能还有其他service没有完成订阅，
            // 此处先对manager更新指令，避免首次订阅无效的问题。
            commandServiceListener.setRpcCommandCache(commandStr);
            log.info("Discovered the command [{}] for url [{}]", commandStr, url);
        } else {
            urls = discoverProviders(urlCopy);
            log.info("Discovered the provider urls [{}] for url [{}]", urls, url);
        }
        return urls;
    }

    /**
     * Get or put command service listener from or to cache
     *
     * @param url client url
     * @return command service listener
     */
    private CommandServiceListener getCommandServiceListener(Url url) {
        CommandServiceListener manager = commandServiceListenerCacheMap.get(url);
        if (manager == null) {
            // Pass the specified registry instance to CommandServiceManager, e.g, ZookeeperRegistry
            manager = new CommandServiceListener(url, this);
            CommandServiceListener serviceManager = commandServiceListenerCacheMap.putIfAbsent(url, manager);
            if (serviceManager != null) {
                // Key exists in map, return old data
                manager = serviceManager;
            }
        }
        return manager;
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

    protected abstract void subscribeServiceListener(Url url, ServiceListener listener);

    protected abstract void unsubscribeServiceListener(Url url, ServiceListener listener);

    protected abstract void subscribeCommandListener(Url url, CommandListener listener);

    protected abstract void unsubscribeCommandListener(Url url, CommandListener listener);

    protected abstract List<Url> discoverProviders(Url url);

    protected abstract String readCommand(Url url);
}
