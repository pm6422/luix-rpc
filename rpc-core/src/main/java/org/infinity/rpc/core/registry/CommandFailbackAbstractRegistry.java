package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.NotifyListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class CommandFailbackAbstractRegistry extends FailbackAbstractRegistry {
    private ConcurrentHashMap<Url, CommandServiceManager> commandManagerMap;

    public CommandFailbackAbstractRegistry(Url url) {
        super(url);
        commandManagerMap = new ConcurrentHashMap<Url, CommandServiceManager>();
        log.info("CommandFailbackRegistry init. url: " + url.toSimpleString());
    }

    protected void doSubscribe(Url url, final NotifyListener listener) {
        log.info("CommandFailbackRegistry subscribe. url: " + url.toSimpleString());
        Url urlCopy = url.copy();
        CommandServiceManager manager = getCommandServiceManager(urlCopy);
        manager.addNotifyListener(listener);

        subscribeServiceListener(urlCopy, manager);
        subscribeCommandListener(urlCopy, manager);

        List<Url> urls = doDiscover(urlCopy);
        if (urls != null && urls.size() > 0) {
            this.notify(urlCopy, listener, urls);
        }
    }

    protected void doUnsubscribe(Url url, NotifyListener listener) {
        log.info("CommandFailbackRegistry unsubscribe. url: " + url.toSimpleString());
        Url urlCopy = url.copy();
        CommandServiceManager manager = commandManagerMap.get(urlCopy);

        manager.removeNotifyListener(listener);
        unsubscribeServiceListener(urlCopy, manager);
        unsubscribeCommandListener(urlCopy, manager);

    }

    protected List<Url> doDiscover(Url url) {
        log.info("CommandFailbackRegistry discover. url: " + url.toSimpleString());
        List<Url> finalResult;

        Url urlCopy = url.copy();
        String commandStr = discoverCommand(urlCopy);
        RpcCommand rpcCommand = null;
        if (StringUtils.isNotEmpty(commandStr)) {
            rpcCommand = RpcCommandUtils.stringToCommand(commandStr);

        }

        log.info("CommandFailbackRegistry discover command. commandStr: " + commandStr + ", rpccommand "
                + (rpcCommand == null ? "is null." : "is not null."));

        if (rpcCommand != null) {
            rpcCommand.sort();
            CommandServiceManager manager = getCommandServiceManager(urlCopy);
            finalResult = manager.discoverServiceWithCommand(urlCopy, new HashMap<String, Integer>(), rpcCommand);

            // 在subscribeCommon时，可能订阅完马上就notify，导致首次notify指令时，可能还有其他service没有完成订阅，
            // 此处先对manager更新指令，避免首次订阅无效的问题。
            manager.setCommandCache(commandStr);
        } else {
            finalResult = discoverProviders(urlCopy);
        }

        log.info("CommandFailbackRegistry discover size: " + (finalResult == null ? "0" : finalResult.size()));

        return finalResult;
    }

    public List<Url> commandPreview(Url url, RpcCommand rpcCommand, String previewIP) {
        List<Url> finalResult;
        Url urlCopy = url.copy();

        if (rpcCommand != null) {
            CommandServiceManager manager = getCommandServiceManager(urlCopy);
            finalResult = manager.discoverServiceWithCommand(urlCopy, new HashMap<String, Integer>(), rpcCommand, previewIP);
        } else {
            finalResult = discoverProviders(urlCopy);
        }

        return finalResult;
    }

    private CommandServiceManager getCommandServiceManager(Url urlCopy) {
        CommandServiceManager manager = commandManagerMap.get(urlCopy);
        if (manager == null) {
            manager = new CommandServiceManager(urlCopy);
            manager.setRegistry(this);
            CommandServiceManager manager1 = commandManagerMap.putIfAbsent(urlCopy, manager);
            if (manager1 != null) manager = manager1;
        }
        return manager;
    }

    // for UnitTest
    public ConcurrentHashMap<Url, CommandServiceManager> getCommandManagerMap() {
        return commandManagerMap;
    }

    protected abstract void subscribeServiceListener(Url url, ServiceListener listener);

    protected abstract void unsubscribeServiceListener(Url url, ServiceListener listener);

    protected abstract void subscribeCommandListener(Url url, CommandListener listener);

    protected abstract void unsubscribeCommandListener(Url url, CommandListener listener);

    protected abstract List<Url> discoverProviders(Url url);

    protected abstract String discoverCommand(Url url);
}
