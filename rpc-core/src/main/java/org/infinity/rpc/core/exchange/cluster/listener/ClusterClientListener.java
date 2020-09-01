package org.infinity.rpc.core.exchange.cluster.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.request.Requester;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.url.Url;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ThreadSafe
public class ClusterClientListener<T> implements ClientListener {
    private       Protocol                     protocol;
    private       Cluster<T>                   cluster;
    private       List<Url>                    registryUrls;
    private       Url                          clientUrl;
    private       Class<T>                     interfaceClass;
    private final Map<Url, List<Requester<T>>> registryRequestersPerRegistryUrl = new ConcurrentHashMap<>();

    public ClusterClientListener(Class<T> interfaceClass, List<Url> registryUrls, Url clientUrl) {
        this.interfaceClass = interfaceClass;
        this.registryUrls = registryUrls;
        this.clientUrl = clientUrl;
        this.protocol = Protocol.getInstance(clientUrl.getProtocol());
        init();
    }

    private void init() {
        initCluster();
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            registry.subscribe(clientUrl, this);
        }
    }

    private void initCluster() {
        String clusterName = clientUrl.getParameter(Url.PARAM_CLUSTER, Url.PARAM_CLUSTER_DEFAULT_VALUE);
        String loadBalancerName = clientUrl.getParameter(Url.PARAM_LOAD_BALANCER, Url.PARAM_LOAD_BALANCER_DEFAULT_VALUE);
        String haName = clientUrl.getParameter(Url.PARAM_HA, Url.PARAM_HA_DEFAULT_VALUE);

        cluster = Cluster.createCluster(clusterName, loadBalancerName, haName, clientUrl);
    }

    @Override
    public synchronized void onSubscribe(Url registryUrl, List<Url> providerUrls) {
        if (CollectionUtils.isEmpty(providerUrls)) {
            onRegistryEmpty(registryUrl);
            log.warn("ClusterSupport config change notify, urls is empty: registry={} service={} urls=[]", registryUrl.getUri(), clientUrl.getIdentity());
            return;
        }

        List<Requester<T>> newRequesters = new ArrayList<>();
        for (Url providerUrl : providerUrls) {
            Requester<T> requester = getExistingRequester(providerUrl, registryRequestersPerRegistryUrl.get(registryUrl));
            if (requester == null) {
                Url providerUrlCopy = providerUrl.copy();
                requester = protocol.createRequester(interfaceClass, providerUrlCopy);
            }
            if (requester != null) {
                newRequesters.add(requester);
            }
        }

        if (CollectionUtils.isEmpty(newRequesters)) {
            onRegistryEmpty(registryUrl);
            return;
        }

        // 此处不销毁referers，由cluster进行销毁
        registryRequestersPerRegistryUrl.put(registryUrl, newRequesters);
        refreshCluster();
    }

    private Requester<T> getExistingRequester(Url providerUrl, List<Requester<T>> requesters) {
        if (requesters == null) {
            return null;
        }
        for (Requester<T> requester : requesters) {
            if (Objects.equals(providerUrl, requester.getProviderUrl())) {
                return requester;
            }
        }
        return null;
    }

    private synchronized void onRegistryEmpty(Url excludeRegistryUrl) {
        boolean noMoreOtherRefers = registryRequestersPerRegistryUrl.size() == 1 && registryRequestersPerRegistryUrl.containsKey(excludeRegistryUrl);
        if (noMoreOtherRefers) {
            log.warn(String.format("Ignore notify for no more referers in this cluster, registry: %s, cluster=%s", excludeRegistryUrl, clientUrl));
        } else {
            registryRequestersPerRegistryUrl.remove(excludeRegistryUrl);
            refreshCluster();
        }
    }

    private synchronized void refreshCluster() {
        List<Requester<T>> requesters = new ArrayList<>();
        for (List<Requester<T>> refs : registryRequestersPerRegistryUrl.values()) {
            requesters.addAll(refs);
        }
        cluster.onRefresh(requesters);
    }
}
