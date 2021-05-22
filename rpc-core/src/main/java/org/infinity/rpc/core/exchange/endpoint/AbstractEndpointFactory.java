package org.infinity.rpc.core.exchange.endpoint;


import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.exchange.checkhealth.HealthChecker;
import org.infinity.rpc.core.exchange.client.Client;
import org.infinity.rpc.core.exchange.endpoint.impl.CheckHealthClientEndpointManager;
import org.infinity.rpc.core.exchange.server.Server;
import org.infinity.rpc.core.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.constant.ProtocolConstants.SHARED_CHANNEL;
import static org.infinity.rpc.core.constant.ProtocolConstants.SHARED_CHANNEL_VAL_DEFAULT;

/**
 * abstract endpoint factory
 *
 * <pre>
 * 		一些约定：
 *
 * 		1） service :
 * 			1.1） not share channel :  某个service暴露服务的时候，不期望和别的service共享服务，明哲自保，比如你说：我很重要，我很重要。
 *
 * 			1.2） share channel ： 某个service 暴露服务的时候，如果有某个模块，但是拆成10个接口，可以使用这种方式，不过有一些约束条件：接口的几个serviceConfig配置需要保持一致。
 *
 * 				不允许差异化的配置如下：
 * 					protocol, codec , serialize, maxContentLength , maxServerConnection , maxWorkerThread, workerQueueSize, heartbeatFactory
 *
 * 		2）心跳机制：
 *
 * 			不同的protocol的心跳包格式可能不一样，无法进行强制，那么通过可扩展的方式，依赖heartbeatFactory进行heartbeat包的创建，
 * 			同时对于service的messageHandler进行wrap heartbeat包的处理。
 *
 * 			对于service来说，把心跳包当成普通的request处理，因为这种heartbeat才能够探测到整个service处理的关键路径的可用状况
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
    public Server createServer(Url providerUrl, MessageHandler messageHandler) {
        messageHandler = HealthChecker.getInstance(providerUrl).wrapMessageHandler(messageHandler);

        synchronized (ipPort2ServerShareChannel) {
            String ipPort = providerUrl.getAddress();
            String protocolKey = RpcFrameworkUtils.getProtocolKey(providerUrl);

            boolean shareChannel = providerUrl.getBooleanOption(SHARED_CHANNEL, SHARED_CHANNEL_VAL_DEFAULT);
            if (!shareChannel) {
                // 独享一个端口
                log.info(this.getClass().getSimpleName() + " create no_share_channel server: url={}", providerUrl);

                // 如果端口已经被使用了，使用该server bind会有异常
                return innerCreateServer(providerUrl, messageHandler);
            }

            log.info(this.getClass().getSimpleName() + " create share_channel server: url={}", providerUrl);

            Server server = ipPort2ServerShareChannel.get(ipPort);

            if (server != null) {
                // can't share service channel
                if (!RpcFrameworkUtils.checkIfCanShareServiceChannel(server.getProviderUrl(), providerUrl)) {
                    throw new RpcFrameworkException(
                            "Service export Error: share channel but some config param is different, " +
                                    "protocol or codec or serialize or maxContentLength or maxServerConnection or maxWorkerThread or heartbeatFactory, source="
                                    + server.getProviderUrl() + " target=" + providerUrl);
                }

                saveEndpoint2Urls(server2UrlsShareChannel, server, protocolKey);

                return server;
            }

            providerUrl = providerUrl.copy();
            // 共享server端口，由于有多个interfaces存在，所以把path设置为空
            providerUrl.setPath("");

            server = innerCreateServer(providerUrl, messageHandler);

            ipPort2ServerShareChannel.put(ipPort, server);
            saveEndpoint2Urls(server2UrlsShareChannel, server, protocolKey);

            return server;
        }
    }

    @Override
    public Client createClient(Url providerUrl) {
        log.info(this.getClass().getSimpleName() + " create client: url={}", providerUrl);
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
        boolean shareChannel = url.getBooleanOption(SHARED_CHANNEL, SHARED_CHANNEL_VAL_DEFAULT);

        if (!shareChannel) {
            destroy(endpoint);
            return;
        }

        synchronized (ipPort2Endpoint) {
            String ipPort = url.getAddress();
            String protocolKey = RpcFrameworkUtils.getProtocolKey(url);

            if (endpoint != ipPort2Endpoint.get(ipPort)) {
                destroy(endpoint);
                return;
            }

            Set<String> urls = endpoint2Urls.get(endpoint);
            urls.remove(protocolKey);

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

    protected abstract Server innerCreateServer(Url url, MessageHandler messageHandler);

    protected abstract Client innerCreateClient(Url providerUrl);

}
