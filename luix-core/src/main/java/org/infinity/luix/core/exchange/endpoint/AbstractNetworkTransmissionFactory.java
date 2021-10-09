package org.infinity.luix.core.exchange.endpoint;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.checkhealth.HealthChecker;
import org.infinity.luix.core.exchange.client.Client;
import org.infinity.luix.core.exchange.endpoint.impl.CheckHealthClientEndpointManager;
import org.infinity.luix.core.exchange.server.Server;
import org.infinity.luix.core.server.messagehandler.ServerInvocationHandleable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.core.utils.RpcFrameworkUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.luix.core.constant.ProtocolConstants.SHARED_SERVER;
import static org.infinity.luix.core.constant.ProtocolConstants.SHARED_SERVER_VAL_DEFAULT;

/**
 * non-shared channel: 某个service暴露服务的时候，不期望和别的service共享服务，明哲自保，比如你说：我很重要，我很重要。
 * shared channel: 某个service暴露服务的时候，如果有某个模块，但是拆成10个接口，可以使用这种方式，不过有一些约束条件：接口的几个serviceConfig配置需要保持一致。
 * 不允许差异化的配置如下：protocol, codec, serializer, maxContentLength, maxServerConnection, maxWorkerThread, workerQueueSize, healthChecker
 */
@Slf4j
public abstract class AbstractNetworkTransmissionFactory implements NetworkTransmissionFactory {

    private final          EndpointManager          endpointManager;
    protected static final Map<String, Server>      ADDRESS_2_SHARED_SERVER          = new ConcurrentHashMap<>();
    protected static final Map<Server, Set<String>> SHARED_SERVER_2_PROVIDER_KEY_SET = new ConcurrentHashMap<>();

    public AbstractNetworkTransmissionFactory() {
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
        client.close();
        endpointManager.removeEndpoint(client);
        log.info("Destroyed the client for [{}] by [{}]", providerUrl, this.getClass().getSimpleName());
    }

    @Override
    public Server createServer(Url providerUrl, ServerInvocationHandleable handler) {
        handler = HealthChecker.getInstance(providerUrl).wrap(handler);

        synchronized (ADDRESS_2_SHARED_SERVER) {
            String address = providerUrl.getAddress();

            boolean sharedChannel = providerUrl.getBooleanOption(SHARED_SERVER, SHARED_SERVER_VAL_DEFAULT);
            if (!sharedChannel) {
                // Create exclusive channel server
                log.info("Created a exclusive channel server for url [{}] by [{}]", providerUrl, this.getClass().getSimpleName());
                return doCreateServer(providerUrl, handler);
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

                saveProviderKey(sharedServer, RpcFrameworkUtils.getProviderKey(providerUrl));
                return sharedServer;
            }

            Url urlCopy = providerUrl.copy();
            // 共享server端口，由于有多个interfaces存在，所以把path设置为空
            urlCopy.setPath(StringUtils.EMPTY);
            sharedServer = doCreateServer(urlCopy, handler);
            ADDRESS_2_SHARED_SERVER.put(address, sharedServer);
            saveProviderKey(sharedServer, RpcFrameworkUtils.getProviderKey(providerUrl));

            return sharedServer;
        }
    }

    @Override
    public void destroyServer(Server server, Url providerUrl) {
        boolean shareChannel = providerUrl.getBooleanOption(SHARED_SERVER, SHARED_SERVER_VAL_DEFAULT);
        if (!shareChannel) {
            server.close();
            return;
        }

        synchronized (ADDRESS_2_SHARED_SERVER) {
            String address = providerUrl.getAddress();
            String providerKey = RpcFrameworkUtils.getProviderKey(providerUrl);

            if (server != ADDRESS_2_SHARED_SERVER.get(address)) {
                server.close();
                return;
            }

            Set<String> providerKeys = SHARED_SERVER_2_PROVIDER_KEY_SET.get(server);
            providerKeys.remove(providerKey);

            if (CollectionUtils.isEmpty(providerKeys)) {
                server.close();
                ADDRESS_2_SHARED_SERVER.remove(address);
                SHARED_SERVER_2_PROVIDER_KEY_SET.remove(server);
            }
        }
    }

    private void saveProviderKey(Server server, String providerKey) {
        synchronized (SHARED_SERVER_2_PROVIDER_KEY_SET) {
            Set<String> providerKeys = SHARED_SERVER_2_PROVIDER_KEY_SET.get(server);
            if (providerKeys == null) {
                providerKeys = new HashSet<>();
                providerKeys.add(providerKey);
                SHARED_SERVER_2_PROVIDER_KEY_SET.putIfAbsent(server, providerKeys);
            }
            providerKeys.add(providerKey);
        }
    }

    protected abstract Client doCreateClient(Url providerUrl);

    protected abstract Server doCreateServer(Url url, ServerInvocationHandleable handler);
}
