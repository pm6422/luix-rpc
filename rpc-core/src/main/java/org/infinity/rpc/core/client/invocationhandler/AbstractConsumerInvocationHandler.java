package org.infinity.rpc.core.client.invocationhandler;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.ratelimit.RateLimiter;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.utils.RpcRequestIdHolder;

import java.lang.reflect.Method;

import static org.infinity.rpc.core.constant.ConsumerConstants.RATE_LIMITER_GUAVA;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public abstract class AbstractConsumerInvocationHandler<T> {
    protected ConsumerStub<T> consumerStub;

    /**
     * @param request    RPC request
     * @param returnType return type of method
     * @return result of method
     */
    protected Object processRequest(Requestable request, Class<?> returnType) {
        if (limitRate()) {
            log.warn("Rate limiting!");
            return null;
        }
        Responseable response;
//            boolean throwException = true;
        try {
            // Store request id on client side
            RpcRequestIdHolder.setRequestId(request.getRequestId());
            // Call chain: provider invoker cluster call => cluster fault tolerance strategy =>
            // LB select node => provider invoker call
            // Only one server node under one cluster can process the request
            response = consumerStub.getInvokerCluster().invoke(request);
            return response.getResult();
        } catch (Exception ex) {
            throw new RpcServiceException(ex);
        } finally {
            RpcRequestIdHolder.destroy();
        }
    }

    /**
     * Check whether the method is derived from {@link Object} class.
     * e.g, toString, equals, hashCode, finalize
     *
     * @param method method
     * @return {@code true} if it was derived from Object class and {@code false} otherwise
     */
    protected boolean isDerivedFromObject(Method method) {
        if (method.getDeclaringClass().equals(Object.class)) {
            try {
                consumerStub.getInterfaceClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
                return false;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    private boolean limitRate() {
        return consumerStub.isLimitRate() && !RateLimiter.getInstance(RATE_LIMITER_GUAVA).tryAcquire();
    }
}
