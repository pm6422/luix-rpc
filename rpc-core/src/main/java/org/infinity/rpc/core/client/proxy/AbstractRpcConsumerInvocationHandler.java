package org.infinity.rpc.core.client.proxy;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.impl.RequestContext;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.switcher.SwitcherService;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.url.UrlParam;

import java.util.List;

@Slf4j
public abstract class AbstractRpcConsumerInvocationHandler<T> {
    protected List<Cluster<T>>   clusters;
    protected Class<T>           interfaceClass;
    protected String             interfaceName;
    protected SwitcherService    switcherService;
    protected InfinityProperties infinityProperties;

    /**
     * @param request
     * @param returnType
     * @param async
     * @return
     */
    protected Object processRequest(Requestable request, Class returnType, boolean async) {
        RequestContext threadRpcContext = RequestContext.getThreadRpcContext();
        threadRpcContext.setAsyncCall(async);

        // Copy values from context to request object
        copyFromContextToRequest(threadRpcContext, request);

//        RequestContext.initialize(request);

        // 当配置多个protocol的时候，比如A,B,C，
        // 那么正常情况下只会使用A，如果A被开关降级，那么就会使用B，B也被降级，那么会使用C
        for (Cluster<T> cluster : clusters) {
            Url clientUrl = cluster.getHighAvailability().getClientUrl();

            request.attachment(Url.PARAM_APP, infinityProperties.getApplication().getName());

            Responseable response = null;
            boolean throwException = Boolean.parseBoolean(clientUrl.getParameter(UrlParam.throwException.getName(), UrlParam.throwException.getValue()));
            try {
                // Cluster call => HA call => requester call
                response = cluster.call(request);
                return response.getResult();
            } catch (Exception ex) {
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
    private void copyFromContextToRequest(RequestContext threadRpcContext, Requestable request) {
        // Copy attachments from RPC context to request object
        threadRpcContext.getAttachments().entrySet().forEach(entry -> request.attachment(entry.getKey(), entry.getValue()));

        // Copy client request id from RPC context to request object
        request.clientRequestId(threadRpcContext.getClientRequestId());
    }

}
