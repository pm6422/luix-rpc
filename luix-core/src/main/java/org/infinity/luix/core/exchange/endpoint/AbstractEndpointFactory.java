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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * abstract endpoint factory
 *
 * <pre>
 * 		一些约定：
 * 		1） service :
 * 			1.1） non-shared channel: 某个service暴露服务的时候，不期望和别的service共享服务，明哲自保，比如你说：我很重要，我很重要。
 *
 * 			1.2） shared channel: 某个service暴露服务的时候，如果有某个模块，但是拆成10个接口，可以使用这种方式，不过有一些约束条件：接口的几个serviceConfig配置需要保持一致。
 *
 * 				不允许差异化的配置如下：
 * 					protocol, codec, serializer, maxContentLength, maxServerConnection, maxWorkerThread, workerQueueSize, heartbeatFactory
 *
 * 		2）心跳机制：
 *
 * 			不同的protocol的心跳包格式可能不一样，无法进行强制，那么通过可扩展的方式，依赖heartbeatFactory进行heartbeat包的创建，
 * 			同时对于service的messageHandler进行wrap heartbeat包的处理。
 * 			对于service来说把心跳包当成普通的request处理，因为这种heartbeat才能够探测到整个service处理的关键路径的可用状况
 *
 * </pre>
 */
@Slf4j
public abstract class AbstractEndpointFactory implements EndpointFactory {

    /**
     * 维持share channel 的service列表
     **/
    protected Map<String, Server>      ipPort2ServerShareChannel = new HashMap<>();
    protected Map<Server, Set<String>> server2UrlsShareChannel   = new ConcurrentHashMap<>();

    private final EndpointManager heartbeatClientEndpointManager;

    public AbstractEndpointFactory() {
        heartbeatClientEndpointManager = new CheckHealthClientEndpointManager();
        heartbeatClientEndpointManager.init();
    }

    @Override
    public Server createServer(Url providerUrl, ProviderInvocationHandleable providerInvocationHandleable) {
        providerInvocationHandleable = HealthChecker.getInstance(providerUrl).wrapMessageHandler(providerInvocationHandleable);

        synchronized (ipPort2ServerShareChannel) {
            String ipPort = providerUrl.getAddress();
            String providerKey = RpcFrameworkUtils.getProviderKey(providerUrl);

            boolean shareChannel = providerUrl.getBooleanOption(ProtocolConstants.SHARED_CHANNEL, ProtocolConstants.SHARED_CHANNEL_VAL_DEFAULT);
            if (!shareChannel) {
                // 独享一个端口
                log.info("Created a exclusive channel server for url [{}] by [{}]", providerUrl, this.getClass().getSimpleName());

                // 如果端口已经被使用了，使用该server bind会有异常
                return innerCreateServer(providerUrl, providerInvocationHandleable);
            }

            log.info("Created a shared channel server for url [{}] by [{}]", providerUrl, this.getClass().getSimpleName());
            Server server = ipPort2ServerShareChannel.get(ipPort);

            if (server != null) {
                // can't share service channel
                if (!RpcFrameworkUtils.checkIfCanShareServiceChannel(server.getProviderUrl(), providerUrl)) {
                    throw new RpcFrameworkException(
                            "Failed to create channel server for incorrect configuration parameter, e.g. " +
                                    "protocol or codec or serialize or maxContentLength or maxServerConnection or maxWorkerThread or heartbeatFactory, " +
                                    "source=" + server.getProviderUrl() + " target=" + providerUrl);
                }

                saveEndpoint2Urls(server2UrlsShareChannel, server, providerKey);

                return server;
            }

            Url copyUrl = providerUrl.copy();
            // 共享server端口，由于有多个interfaces存在，所以把path设置为空
            copyUrl.setPath(StringUtils.EMPTY);
            server = innerCreateServer(copyUrl, providerInvocationHandleable);
            ipPort2ServerShareChannel.put(ipPort, server);
            saveEndpoint2Urls(server2UrlsShareChannel, server, providerKey);

            return server;
        }
    }

    @Override
    public Client createClient(Url providerUrl) {
        log.info("Created a client for url [{}] by [{}]", providerUrl, this.getClass().getSimpleName());
        return createClient(providerUrl, heartbeatClientEndpointManager);
    }

    @Override
    public void safeReleaseResource(Server server, Url providerUrl) {
        safeReleaseResource(server, providerUrl, ipPort2ServerShareChannel, server2UrlsShareChannel);
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
            heartbeatClientEndpointManager.removeEndpoint(endpoint);
        } else {
            endpoint.close();
        }
    }

    public Map<String, Server> getShallServerChannels() {
        return Collections.unmodifiableMap(ipPort2ServerShareChannel);
    }

    public EndpointManager getEndpointManager() {
        return heartbeatClientEndpointManager;
    }

    protected abstract Server innerCreateServer(Url url, ProviderInvocationHandleable providerInvocationHandleable);

    protected abstract Client innerCreateClient(Url providerUrl);

}
