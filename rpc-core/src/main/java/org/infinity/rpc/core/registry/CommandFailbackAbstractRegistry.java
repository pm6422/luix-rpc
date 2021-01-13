package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.subscribe.RpcCommand;
import org.infinity.rpc.core.subscribe.RpcCommandUtils;
import org.infinity.rpc.core.url.Url;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@NotThreadSafe
public abstract class CommandFailbackAbstractRegistry extends FailbackAbstractRegistry {
    private final Map<Url, CommandServiceListener> commandServiceListenerPerClientUrl = new ConcurrentHashMap<>();

    public CommandFailbackAbstractRegistry(Url registryUrl) {
        super(registryUrl);
    }

    public Map<Url, CommandServiceListener> getCommandServiceListenerPerClientUrl() {
        return commandServiceListenerPerClientUrl;
    }

    /**
     * It contains the functionality of method subscribeServiceListener and subscribeCommandListener
     * And execute the listener
     *
     * @param clientUrl client url
     * @param listener  client listener
     */
    @Override
    protected void doSubscribe(Url clientUrl, final ClientListener listener) {
        Url clientUrlCopy = clientUrl.copy();
        // Create a new command service listener or get it from cache
        CommandServiceListener commandServiceListener = getCommandServiceListener(clientUrlCopy);
        // Add client listener to command service listener, and use command service listener to manage listener
        commandServiceListener.addNotifyListener(listener);

        // Trigger onNotify method of commandServiceListener if child change event happens
        subscribeServiceListener(clientUrlCopy, commandServiceListener);
        // Trigger onNotify method of commandServiceListener if data change event happens
        subscribeCommandListener(clientUrlCopy, commandServiceListener);
        // Discover active providers
        List<Url> providerUrls = doDiscover(clientUrlCopy);
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            // Notify discovered providers to client side
            this.notify(providerUrls, clientUrlCopy, listener);
        }
        log.info("Subscribed the listener for the url [{}]", clientUrl);
    }

    /**
     * Unsubscribe the service and command listener
     *
     * @param clientUrl client url
     * @param listener  client listener
     */
    @Override
    protected void doUnsubscribe(Url clientUrl, ClientListener listener) {
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
     * @return urls
     */
    @Override
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
            // Pass the specified registry instance to CommandServiceListener, e.g, ZookeeperRegistry
            listener = new CommandServiceListener(clientUrl, this);
            CommandServiceListener commandServiceListener = commandServiceListenerPerClientUrl.putIfAbsent(clientUrl, listener);
            if (commandServiceListener != null) {
                // Key exists in map, return old data
                listener = commandServiceListener;
            }
        }
        return listener;
    }

    public List<Url> commandPreview(Url url, RpcCommand rpcCommand, String previewIp) {
        List<Url> finalResult;
        Url urlCopy = url.copy();

        if (rpcCommand != null) {
            CommandServiceListener manager = getCommandServiceListener(urlCopy);
            finalResult = manager.discoverServiceWithCommand(urlCopy, new HashMap<>(), rpcCommand, previewIp);
        } else {
            finalResult = discoverActiveProviders(urlCopy);
        }

        return finalResult;
    }

    protected abstract void subscribeCommandListener(Url clientUrl, CommandListener listener);

    protected abstract void unsubscribeCommandListener(Url clientUrl, CommandListener listener);

    protected abstract String readCommand(Url clientUrl);
}
