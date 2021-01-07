package org.infinity.rpc.core.client.proxy;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.client.ConsumerWrapper;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.cluster.ProviderCluster;
import org.infinity.rpc.core.exchange.cluster.ProviderClusterHolder;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.impl.RequestContext;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.switcher.impl.SwitcherService;
import org.infinity.rpc.core.url.Url;

import java.util.List;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public abstract class AbstractRpcConsumerInvocationHandler<T> {
    protected ConsumerWrapper<T> consumerWrapper;
    protected SwitcherService    switcherService;

    /**
     * @param request    RPC request
     * @param returnType return type of method
     * @param async      async call flag
     * @return return result of method
     */
    protected Object processRequest(Requestable request, Class<?> returnType, boolean async) {
        RequestContext threadRpcContext = RequestContext.getThreadRpcContext();
        threadRpcContext.setAsyncCall(async);

        // Copy values from context to request object
        copyContextToRequest(threadRpcContext, request);
//        RequestContext.initialize(request);

        @SuppressWarnings({"unchecked"})
        // This RPC framework supports multiple protocols, one cluster is created for one protocol
        List<ProviderCluster<T>> providerClusters = ProviderClusterHolder.getInstance().getClusters();
        for (ProviderCluster<T> providerCluster : providerClusters) {
            Url clientUrl = Url.clientUrl(providerCluster.getProtocol(), consumerWrapper.getInterfaceClass().getName());
            providerCluster.getFaultToleranceStrategy().setClientUrl(clientUrl);

//            request.addAttachment(Url.PARAM_APP, infinityProperties.getApplication().getName());
            request.setProtocol(providerCluster.getProtocol());

            Responseable response;
//            boolean throwException = true;
            try {
                // Call chain: provider cluster call => cluster fault tolerance strategy =>
                // LB select node => provider caller call
                // Only one server node under one cluster can process the request
                response = providerCluster.call(request);
                return response.getResult();
            } catch (Exception ex) {
                // todo: handle exception
                log.error("", ex);
            }
        }
        throw new RpcServiceException("No cluster!");
    }

    /**
     * Copy values from context to request object
     *
     * @param threadRpcContext RPC context object
     * @param request          request object
     */
    private void copyContextToRequest(RequestContext threadRpcContext, Requestable request) {
        // Copy attachments from RPC context to request object
        threadRpcContext.getAttachments().forEach(request::addAttachment);

        // Copy client request id from RPC context to request object
        request.setClientRequestId(threadRpcContext.getClientRequestId());
    }

}
