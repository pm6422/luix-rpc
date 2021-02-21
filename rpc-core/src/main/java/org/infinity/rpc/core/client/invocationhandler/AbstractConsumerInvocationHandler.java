package org.infinity.rpc.core.client.invocationhandler;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.RpcContext;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.switcher.impl.SwitcherService;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public abstract class AbstractConsumerInvocationHandler<T> {
    protected ConsumerStub<T> consumerStub;
    protected SwitcherService switcherService;

    /**
     * @param request    RPC request
     * @param returnType return type of method
     * @return result of method
     */
    protected Object processRequest(Requestable request, Class<?> returnType) {
        // Create a new {@link RpcContext} for each thread
        RpcContext context = RpcContext.getInstance();
        copy(context, request);

        Responseable response;
//            boolean throwException = true;
        try {
            // Call chain: provider cluster call => cluster fault tolerance strategy =>
            // LB select node => provider caller call
            // Only one server node under one cluster can process the request
            response = consumerStub.getProviderCluster().call(request);
            return response.getResult();
        } catch (Exception ex) {
            throw new RpcServiceException(ex);
        }
    }

    /**
     * Copy values between RPC context and RPC request object
     *
     * @param context RPC context
     * @param request RPC request
     */
    private void copy(RpcContext context, Requestable request) {
        context.setRequestId(request.getRequestId());

        // Copy options from RPC context to RPC request
        context.getOptions().forEach(request::addOption);
    }
}
