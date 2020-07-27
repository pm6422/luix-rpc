package org.infinity.rpc.core.client.proxy;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.request.impl.RequestContext;
import org.infinity.rpc.core.switcher.SwitcherService;

public abstract class AbstractRpcConsumerInvocationHandler<T> {
    protected String          interfaceName;
    protected Class<T>        clazz;
    protected SwitcherService switcherService;

    protected void initialize() {

    }

    protected Object invokeRequest(Requestable request, Class returnType, boolean async) throws Throwable {
        RequestContext threadRpcContext = RequestContext.getThreadRpcContext();
        threadRpcContext.setAsyncCall(async);

        // Copy values from context to request object
        copyFromContextToRequest(threadRpcContext, request);

        return null;
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
