package org.infinity.rpc.core.client.listener;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.invoker.ServiceInvoker;
import org.infinity.rpc.core.protocol.Protocol;
import org.infinity.rpc.core.registry.Registry;
import org.infinity.rpc.core.registry.RegistryFactory;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.annotation.EventSubscriber;
import org.infinity.rpc.utilities.concurrent.ThreadSafe;

import java.util.List;

/**
 * todo: see ClusterSupport
 * Listener used to subscribe providers change event,
 * method {@link ProviderDiscoveryListener#onNotify(Url, List)} will be invoked if providers change event occurs.
 */
@Slf4j
@ThreadSafe
public class ProviderDiscoveryListener extends ProviderNotifyListener {
    private Url consumerUrl;

    /**
     * Prevent instantiation of it outside the class
     */
    private ProviderDiscoveryListener() {
        super();
    }

    /**
     * Pass provider invoker cluster to listener, listener will update provider invoker cluster after provider urls changed
     *
     * @param serviceInvoker    provider invoker cluster
     * @param interfaceName     The interface class name of the consumer
     * @param form              form
     * @param consumerUrl       consumer url
     * @param providerProcessor provider processor
     * @return listener listener
     */
    public static ProviderDiscoveryListener of(ServiceInvoker serviceInvoker,
                                               String interfaceName,
                                               String form,
                                               Url consumerUrl,
                                               ProviderProcessable providerProcessor) {
        ProviderDiscoveryListener listener = new ProviderDiscoveryListener();
        listener.serviceInvoker = serviceInvoker;
        listener.interfaceName = interfaceName;
        listener.form = form;
        listener.consumerUrl = consumerUrl;
        listener.protocol = Protocol.getInstance(consumerUrl.getProtocol());
        listener.providerProcessor = providerProcessor;
        return listener;
    }

    /**
     * IMPORTANT: Subscribe this client listener to all the registries
     * So when providers change event occurs, it can invoke onNotify() method.
     *
     * @param registryUrls registry urls
     */
    @EventSubscriber("providersDiscoveryEvent")
    public void subscribe(List<Url> registryUrls) {
        for (Url registryUrl : registryUrls) {
            Registry registry = RegistryFactory.getInstance(registryUrl.getProtocol()).getRegistry(registryUrl);
            // Bind this listener to the client
            registry.subscribe(consumerUrl, this);
        }
    }

    @Override
    public String toString() {
        return ProviderDiscoveryListener.class.getSimpleName().concat(":").concat(interfaceName);
    }
}
