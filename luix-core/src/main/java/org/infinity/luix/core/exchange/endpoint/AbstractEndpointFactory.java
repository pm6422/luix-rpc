package org.infinity.luix.core.exchange.endpoint;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.checkhealth.HealthChecker;
import org.infinity.luix.core.exchange.client.Client;
import org.infinity.luix.core.exchange.endpoint.impl.CheckHealthClientEndpointManager;
import org.infinity.luix.core.exchange.server.Server;
import org.infinity.luix.core.server.messagehandler.ProviderInvocationHandleable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * non-shared channel: 某个service暴露服务的时候，不期望和别的service共享服务，明哲自保，比如你说：我很重要，我很重要。
 * shared channel: 某个service暴露服务的时候，如果有某个模块，但是拆成10个接口，可以使用这种方式，不过有一些约束条件：接口的几个serviceConfig配置需要保持一致。
 * 不允许差异化的配置如下：protocol, codec, serializer, maxContentLength, maxServerConnection, maxWorkerThread, workerQueueSize, heartbeatFactory
 */
@Slf4j
public abstract class AbstractEndpointFactory implements EndpointFactory {

    private final EndpointManager          checkHealthClientEndpointManager;
    protected     Map<String, Server>      address2ServerSharedChannel = new ConcurrentHashMap<>();
    protected     Map<Server, Set<String>> server2UrlsSharedChannel    = new ConcurrentHashMap<>();

    public AbstractEndpointFactory() {
        checkHealthClientEndpointManager = new CheckHealthClientEndpointManager();
        checkHealthClientEndpointManager.init();
    }

    @Override
    public Server createServer(Url providerUrl, ProviderInvocationHandleable providerInvocationHandleable) {
        providerInvocationHandleable = HealthChecker.getInstance(providerUrl).wrapMessageHandler(providerInvocationHandleable);

        synchronized (address2ServerSharedChannel) {
            String address = providerUrl.getAddress();
            String providerKey = RpcFrameworkUtils.getProviderKey(providerUrl);

            boolean shareChannel = providerUrl.getBooleanOption(ProtocolConstants.SHARED_CHANNEL, ProtocolConstants.SHARED_CHANNEL_VAL_DEFAULT);
            if (!shareChannel) {
                // 独享一个端口
                log.info("Created a exclusive channel server for url [{}] by [{}]", providerUrl, this.getClass().getSimpleName());

                // 如果端口已经被使用了，使用该server bind会有异常
                return innerCreateServer(providerUrl, providerInvocationHandleable);
            }

            log.info("Created a shared channel server for url [{}] by [{}]", providerUrl, this.getClass().getSimpleName());
            Server server = address2ServerSharedChannel.get(address);

            if (server != null) {
                // can't share service channel
                if (!RpcFrameworkUtils.checkIfCanShareServiceChannel(server.getProviderUrl(), providerUrl)) {
                    throw new RpcFrameworkException(
                            "Failed to create channel server for incorrect configuration parameter, e.g. " +
                                    "protocol or codec or serialize or maxContentLength or maxServerConnection or maxWorkerThread or heartbeatFactory, " +
                                    "source=" + server.getProviderUrl() + " target=" + providerUrl);
                }

                saveEndpoint2Urls(server2UrlsSharedChannel, server, providerKey);

                return server;
            }

            Url copyUrl = providerUrl.copy();
            // 共享server端口，由于有多个interfaces存在，所以把path设置为空
            copyUrl.setPath(StringUtils.EMPTY);
            server = innerCreateServer(copyUrl, providerInvocationHandleable);
            address2ServerSharedChannel.put(address, server);
            saveEndpoint2Urls(server2UrlsSharedChannel, server, providerKey);

            return server;
        }
    }

    @Override
    public Client createClient(Url providerUrl) {
        log.info("Created a client for url [{}] by [{}]", providerUrl, this.getClass().getSimpleName());
        return createClient(providerUrl, checkHealthClientEndpointManager);
    }

    @Override
    public void safeReleaseResource(Server server, Url providerUrl) {
        safeReleaseResource(server, providerUrl, address2ServerSharedChannel, server2UrlsSharedChannel);
    }

    @Override
    public void safeReleaseResource(Client client, Url providerUrl) {
        destroy(client);
    }

    private <T extends Endpoint> void safeReleaseResource(T endpoint, Url url, Map<String, T> ipPort2Endpoint,
                                                          Map<T, Set<String>> endpoint2Urls) {
        boolean shareChannel = url.getBooleanOption(ProtocolConstants.SHARED_CHANNEL, ProtocolConstants.SHARED_CHANNEL_VAL_DEFAULT);

        if (!shareChannel) {
            destroy(endpoint);
            return;
        }

        synchronized (ipPort2Endpoint) {
            String ipPort = url.getAddress();
            String providerKey = RpcFrameworkUtils.getProviderKey(url);

            if (endpoint != ipPort2Endpoint.get(ipPort)) {
                destroy(endpoint);
                return;
            }

            Set<String> urls = endpoint2Urls.get(endpoint);
            urls.remove(providerKey);

            if (urls.isEmpty()) {
                destroy(endpoint);
                ipPort2Endpoint.remove(ipPort);
                endpoint2Urls.remove(endpoint);
            }
        }
    }

    private <T> void saveEndpoint2Urls(Map<T, Set<String>> map, T endpoint, String namespace) {
        Set<String> sets = map.get(endpoint);

        if (sets == null) {
            sets = new HashSet<>();
            sets.add(namespace);
            map.putIfAbsent(endpoint, sets); // 规避并发问题，因为有release逻辑存在，所以这里的sets预先add了namespace
            sets = map.get(endpoint);
        }

        sets.add(namespace);
    }

    private Client createClient(Url providerUrl, EndpointManager endpointManager) {
        Client client = innerCreateClient(providerUrl);
        endpointManager.addEndpoint(client);
        return client;
    }

    private <T extends Endpoint> void destroy(T endpoint) {
        if (endpoint instanceof Client) {
            endpoint.close();
            checkHealthClientEndpointManager.removeEndpoint(endpoint);
        } else {
            endpoint.close();
        }
    }

    public Map<String, Server> getSharedServerChannels() {
        return Collections.unmodifiableMap(address2ServerSharedChannel);
    }

    public EndpointManager getEndpointManager() {
        return checkHealthClientEndpointManager;
    }

    protected abstract Server innerCreateServer(Url url, ProviderInvocationHandleable providerInvocationHandleable);

    protected abstract Client innerCreateClient(Url providerUrl);
}
