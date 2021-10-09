package org.infinity.luix.core.exchange.endpoint;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.checkhealth.HealthChecker;
import org.infinity.luix.core.exchange.client.Client;
import org.infinity.luix.core.exchange.endpoint.impl.CheckHealthClientEndpointManager;
import org.infinity.luix.core.exchange.server.Server;
import org.infinity.luix.core.server.messagehandler.ProviderInvocationHandleable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.luix.core.constant.ProtocolConstants.SHARED_CHANNEL;
import static org.infinity.luix.core.constant.ProtocolConstants.SHARED_CHANNEL_VAL_DEFAULT;

/**
 * non-shared channel: 某个service暴露服务的时候，不期望和别的service共享服务，明哲自保，比如你说：我很重要，我很重要。
 * shared channel: 某个service暴露服务的时候，如果有某个模块，但是拆成10个接口，可以使用这种方式，不过有一些约束条件：接口的几个serviceConfig配置需要保持一致。
 * 不允许差异化的配置如下：protocol, codec, serializer, maxContentLength, maxServerConnection, maxWorkerThread, workerQueueSize, healthChecker
 */
@Slf4j
public abstract class AbstractEndpointFactory implements EndpointFactory {

    private final          EndpointManager          endpointManager;
    protected static final Map<String, Server>      ADDRESS_2_SHARED_SERVER          = new ConcurrentHashMap<>();
    protected static final Map<Server, Set<String>> SHARED_SERVER_2_PROVIDER_KEY_SET = new ConcurrentHashMap<>();

    public AbstractEndpointFactory() {
        endpointManager = new CheckHealthClientEndpointManager();
        endpointManager.init();
    }

    @Override
    public Client createClient(Url providerUrl) {
        log.info("Created a client for url [{}] by [{}]", providerUrl, this.getClass().getSimpleName());
        Client client = doCreateClient(providerUrl);
        endpointManager.addEndpoint(client);
        return client;
    }

    @Override
    public void destroyClient(Client client, Url providerUrl) {
        log.info("Destroyed the client for [{}] by [{}]", providerUrl, this.getClass().getSimpleName());
        client.close();
        endpointManager.removeEndpoint(client);
    }

    @Override
    public Server createServer(Url providerUrl, ProviderInvocationHandleable providerInvocationHandler) {
        providerInvocationHandler = HealthChecker.getInstance(providerUrl).wrap(providerInvocationHandler);

        synchronized (ADDRESS_2_SHARED_SERVER) {
            String address = providerUrl.getAddress();

            boolean sharedChannel = providerUrl.getBooleanOption(SHARED_CHANNEL, SHARED_CHANNEL_VAL_DEFAULT);
            if (!sharedChannel) {
                // Create exclusive channel server
                log.info("Created a exclusive channel server for url [{}] by [{}]", providerUrl, this.getClass().getSimpleName());
                return doCreateServer(providerUrl, providerInvocationHandler);
            }

            log.info("Created a shared channel server for url [{}] by [{}]", providerUrl, this.getClass().getSimpleName());
            Server sharedServer = ADDRESS_2_SHARED_SERVER.get(address);
            if (sharedServer != null) {
                if (!RpcFrameworkUtils.checkIfCanShareServiceChannel(sharedServer.getProviderUrl(), providerUrl)) {
                    throw new RpcFrameworkException(
                            "Failed to create channel server for incorrect configuration parameter, e.g. " +
                                    "protocol or codec or serializer or maxContentLength or maxServerConnection " +
                                    "or maxWorkerThread or healthChecker, " +
                                    "source=" + sharedServer.getProviderUrl() + " target=" + providerUrl);
                }

                saveProviderKey(SHARED_SERVER_2_PROVIDER_KEY_SET, sharedServer, RpcFrameworkUtils.getProviderKey(providerUrl));
                return sharedServer;
            }

            Url urlCopy = providerUrl.copy();
            // 共享server端口，由于有多个interfaces存在，所以把path设置为空
            urlCopy.setPath(StringUtils.EMPTY);
            sharedServer = doCreateServer(urlCopy, providerInvocationHandler);
            ADDRESS_2_SHARED_SERVER.put(address, sharedServer);
            saveProviderKey(SHARED_SERVER_2_PROVIDER_KEY_SET, sharedServer, RpcFrameworkUtils.getProviderKey(providerUrl));

            return sharedServer;
        }
    }

    @Override
    public void destroyServer(Server server, Url providerUrl) {
        destroyServer(server, providerUrl, ADDRESS_2_SHARED_SERVER, SHARED_SERVER_2_PROVIDER_KEY_SET);
    }

    private <T extends Endpoint> void destroyServer(T endpoint, Url url, Map<String, T> ipPort2Endpoint, Map<T, Set<String>> endpoint2Urls) {
        boolean shareChannel = url.getBooleanOption(SHARED_CHANNEL, SHARED_CHANNEL_VAL_DEFAULT);
        if (!shareChannel) {
            endpoint.close();
            return;
        }

        synchronized (ipPort2Endpoint) {
            String ipPort = url.getAddress();
            String providerKey = RpcFrameworkUtils.getProviderKey(url);

            if (endpoint != ipPort2Endpoint.get(ipPort)) {
                endpoint.close();
                return;
            }

            Set<String> urls = endpoint2Urls.get(endpoint);
            urls.remove(providerKey);

            if (urls.isEmpty()) {
                endpoint.close();
                ipPort2Endpoint.remove(ipPort);
                endpoint2Urls.remove(endpoint);
            }
        }
    }

    private <T> void saveProviderKey(Map<T, Set<String>> map, T server, String providerKey) {
        synchronized (map) {
            Set<String> providerKeys = map.get(server);
            if (providerKeys == null) {
                providerKeys = new HashSet<>();
                providerKeys.add(providerKey);
                map.putIfAbsent(server, providerKeys);
            }
            providerKeys.add(providerKey);
        }
    }

    protected abstract Client doCreateClient(Url providerUrl);

    protected abstract Server doCreateServer(Url url, ProviderInvocationHandleable providerInvocationHandleable);
}
