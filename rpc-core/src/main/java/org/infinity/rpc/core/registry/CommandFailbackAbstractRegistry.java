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
    private Map<Url, CommandServiceListener> commandServiceListenerPerClientUrl = new ConcurrentHashMap<>();

    public CommandFailbackAbstractRegistry(Url registryUrl) {
        super(registryUrl);
    }

    public Map<Url, CommandServiceListener> getCommandServiceListenerPerClientUrl() {
        return commandServiceListenerPerClientUrl;
    }

    /**
     * It contains the functionality of method subscribeServiceListener and subscribeCommandListener
     *
     * @param clientUrl client url
     * @param listener  notify listener
     */
    protected void doSubscribe(Url clientUrl, final NotifyListener listener) {
        Url clientUrlCopy = clientUrl.copy();
        // Get or put command service listener from or to cache
        CommandServiceListener commandServiceListener = getCommandServiceListener(clientUrlCopy);
        // Add notify listener to command service listener
        commandServiceListener.addNotifyListener(listener);

        // Trigger onSubscribe method of commandServiceListener if child change event happens
        subscribeServiceListener(clientUrlCopy, commandServiceListener);
        // Trigger onSubscribe method of commandServiceListener if data change event happens
        subscribeCommandListener(clientUrlCopy, commandServiceListener);

        List<Url> providerUrls = doDiscover(clientUrlCopy);
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            this.notify(clientUrlCopy, listener, providerUrls);
        }
        log.info("Subscribed the listener for the url [{}]", clientUrl);
    }

    /**
     * Unsubscribe the service and command listener
     *
     * @param clientUrl client url
     * @param listener  notify listener
     */
    protected void doUnsubscribe(Url clientUrl, NotifyListener listener) {
        Url urlCopy = clientUrl.copy();
        CommandServiceListener commandServiceListener = commandServiceListenerPerClientUrl.get(urlCopy);
        // Remove notify listener from command service listener
        commandServiceListener.removeNotifyListener(listener);
        // Unsubscribe service listener
        unsubscribeServiceListener(urlCopy, commandServiceListener);
        // Unsubscribe command listener
        unsubscribeCommandListener(urlCopy, commandServiceListener);
        log.info("Unsubscribed the listener for the url [{}]", clientUrl);
    }

    /**
     * Discover the provider or command url
     *
     * @param clientUrl url
     * @return
     */
    protected List<Url> doDiscover(Url clientUrl) {
        List<Url> urls;

        Url urlCopy = clientUrl.copy();
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
            log.info("Discovered the command [{}] for url [{}]", commandStr, clientUrl);
        } else {
            urls = discoverActiveProviders(urlCopy);
            log.info("Discovered the provider urls [{}] for url [{}]", urls, clientUrl);
        }
        return urls;
    }

    /**
     * Get or put command service listener from or to cache
     *
     * @param clientUrl client url
     * @return command service listener
     */
    private CommandServiceListener getCommandServiceListener(Url clientUrl) {
        CommandServiceListener listener = commandServiceListenerPerClientUrl.get(clientUrl);
        if (listener == null) {
            // Pass the specified registry instance to CommandServiceManager, e.g, ZookeeperRegistry
            listener = new CommandServiceListener(clientUrl, this);
            CommandServiceListener serviceManager = commandServiceListenerPerClientUrl.putIfAbsent(clientUrl, listener);
            if (serviceManager != null) {
                // Key exists in map, return old data
                listener = serviceManager;
            }
        }
        return listener;
    }

    public List<Url> commandPreview(Url url, RpcCommand rpcCommand, String previewIP) {
        List<Url> finalResult;
        Url urlCopy = url.copy();

        if (rpcCommand != null) {
            CommandServiceListener manager = getCommandServiceListener(urlCopy);
            finalResult = manager.discoverServiceWithCommand(urlCopy, new HashMap<String, Integer>(), rpcCommand, previewIP);
        } else {
            finalResult = discoverActiveProviders(urlCopy);
        }

        return finalResult;
    }

    protected abstract void subscribeServiceListener(Url clientUrl, ServiceListener listener);

    protected abstract void unsubscribeServiceListener(Url clientUrl, ServiceListener listener);

    protected abstract void subscribeCommandListener(Url clientUrl, CommandListener listener);

    protected abstract void unsubscribeCommandListener(Url clientUrl, CommandListener listener);

    protected abstract List<Url> discoverActiveProviders(Url clientUrl);

    protected abstract String readCommand(Url clientUrl);
}
