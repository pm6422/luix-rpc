package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.subscribe.RpcCommand;
import org.infinity.rpc.core.subscribe.RpcCommandUtils;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.concurrent.NotThreadSafe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@NotThreadSafe
public abstract class CommandFailbackAbstractRegistry extends FailbackAbstractRegistry {
    private final Map<Url, CommandProviderListener> commandServiceListenerPerConsumerUrl = new ConcurrentHashMap<>();

    public CommandFailbackAbstractRegistry(Url registryUrl) {
        super(registryUrl);
    }

    public Map<Url, CommandProviderListener> getCommandServiceListenerPerConsumerUrl() {
        return commandServiceListenerPerConsumerUrl;
    }

    /**
     * It contains the functionality of method subscribeServiceListener and subscribeCommandListener
     * And execute the listener
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    @Override
    protected void doSubscribe(Url consumerUrl, final ClientListener listener) {
        Url consumerUrlCopy = consumerUrl.copy();
        // Create a new command service listener or get it from cache
        CommandProviderListener commandServiceListener = getCommandServiceListener(consumerUrlCopy);
        // Add client listener to command service listener, and use command service listener to manage listener
        commandServiceListener.addNotifyListener(listener);

        // Trigger onNotify method of commandServiceListener if child change event happens
        subscribeProviderListener(consumerUrlCopy, commandServiceListener);
        // Trigger onNotify method of commandServiceListener if data change event happens
        subscribeCommandListener(consumerUrlCopy, commandServiceListener);
        // Discover active providers
        List<Url> providerUrls = doDiscover(consumerUrlCopy);
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            // Notify discovered providers to client side
            this.notify(providerUrls, consumerUrlCopy, listener);
        }
        log.info("Subscribed the listener for the url [{}]", consumerUrl);
    }

    /**
     * Unsubscribe the service and command listener
     *
     * @param consumerUrl consumer url
     * @param listener    client listener
     */
    @Override
    protected void doUnsubscribe(Url consumerUrl, ClientListener listener) {
        Url urlCopy = consumerUrl.copy();
        CommandProviderListener commandServiceListener = commandServiceListenerPerConsumerUrl.get(urlCopy);
        // Remove notify listener from command service listener
        commandServiceListener.removeNotifyListener(listener);
        // Unsubscribe service listener
        unsubscribeProviderListener(urlCopy, commandServiceListener);
        // Unsubscribe command listener
        unsubscribeCommandListener(urlCopy, commandServiceListener);
        log.info("Unsubscribed the listener for the url [{}]", consumerUrl);
    }

    /**
     * Discover the provider or command url
     *
     * @param consumerUrl consumer url
     * @return provider urls
     */
    @Override
    protected List<Url> doDiscover(Url consumerUrl) {
        List<Url> providerUrls;

        Url urlCopy = consumerUrl.copy();
        // Read command json content of specified url
        String commandStr = readCommand(urlCopy);
        RpcCommand rpcCommand = null;
        if (StringUtils.isNotEmpty(commandStr)) {
            rpcCommand = RpcCommandUtils.convertToCommand(commandStr);
        }

        if (rpcCommand != null) {
            rpcCommand.sort();
            CommandProviderListener commandServiceListener = getCommandServiceListener(urlCopy);
            providerUrls = commandServiceListener.discoverServiceWithCommand(urlCopy, new HashMap<>(), rpcCommand);
            // 在subscribeCommon时，可能订阅完马上就notify，导致首次notify指令时，可能还有其他service没有完成订阅，
            // 此处先对manager更新指令，避免首次订阅无效的问题。
            commandServiceListener.setRpcCommandCache(commandStr);
            log.info("Discovered the command [{}] for url [{}]", commandStr, consumerUrl);
        } else {
            providerUrls = discoverActiveProviders(urlCopy);
        }
        if (CollectionUtils.isNotEmpty(providerUrls)) {
            log.info("Discovered the provider urls [{}] for url [{}]", providerUrls, consumerUrl);
        } else {
            log.warn("No RPC service providers found on registry for consumer url [{}]!", consumerUrl);
        }
        return providerUrls;
    }

    /**
     * Get or put command service listener from or to cache
     *
     * @param consumerUrl consumer url
     * @return command service listener
     */
    private CommandProviderListener getCommandServiceListener(Url consumerUrl) {
        CommandProviderListener listener = commandServiceListenerPerConsumerUrl.get(consumerUrl);
        if (listener == null) {
            // Pass the specified registry instance to CommandServiceListener, e.g, ZookeeperRegistry
            listener = new CommandProviderListener(consumerUrl, this);
            CommandProviderListener commandServiceListener = commandServiceListenerPerConsumerUrl.putIfAbsent(consumerUrl, listener);
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
            CommandProviderListener manager = getCommandServiceListener(urlCopy);
            finalResult = manager.discoverServiceWithCommand(urlCopy, new HashMap<>(), rpcCommand, previewIp);
        } else {
            finalResult = discoverActiveProviders(urlCopy);
        }

        return finalResult;
    }

    protected abstract void subscribeCommandListener(Url consumerUrl, CommandListener listener);

    protected abstract void unsubscribeCommandListener(Url consumerUrl, CommandListener listener);

    protected abstract String readCommand(Url consumerUrl);
}
