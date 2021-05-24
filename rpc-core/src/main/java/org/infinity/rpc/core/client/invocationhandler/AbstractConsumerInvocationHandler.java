package org.infinity.rpc.core.client.invocationhandler;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.ratelimit.RateLimiter;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.request.impl.RpcRequest;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.core.utils.RpcConfigValidator;
import org.infinity.rpc.core.utils.RpcRequestIdHolder;

import java.lang.reflect.Method;

import static org.infinity.rpc.core.constant.ConsumerConstants.RATE_LIMITER_GUAVA;
import static org.infinity.rpc.core.constant.ProtocolConstants.THROW_EXCEPTION;
import static org.infinity.rpc.core.constant.ProtocolConstants.THROW_EXCEPTION_VAL_DEFAULT;
import static org.infinity.rpc.core.constant.ServiceConstants.*;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public abstract class AbstractConsumerInvocationHandler<T> {
    protected ConsumerStub<T> consumerStub;

    /**
     * @param request    RPC request
     * @param args       method arguments
     * @param returnType return type of method
     * @return result of method
     */
    protected Object process(RpcRequest request, Object[] args, Class<?> returnType) {
        // Set method arguments
        request.setMethodArguments(args);

        // Set some options
        request.addOption(FORM, consumerStub.getForm());
        request.addOption(VERSION, consumerStub.getVersion());
        request.addOption(HEALTH_CHECKER, consumerStub.getHealthChecker());
        request.addOption(REQUEST_TIMEOUT, consumerStub.getRequestTimeout(), REQUEST_TIMEOUT_VAL_DEFAULT);
        request.addOption(MAX_RETRIES, consumerStub.getMaxRetries(), MAX_RETRIES_VAL_DEFAULT);
        request.addOption(MAX_PAYLOAD, consumerStub.getMaxPayload(), MAX_PAYLOAD_VAL_DEFAULT);

        return processRequest(request, returnType);
    }


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

        RpcConfigValidator.notNull(consumerStub.getInvokerCluster(), "Incorrect consumer stub configuration!");

        Responseable response;
        try {
            // Store request id on client side
            RpcRequestIdHolder.setRequestId(request.getRequestId());
            // Call chain: provider invoker cluster call => cluster fault tolerance strategy =>
            // LB select node => provider invoker call
            // Only one server node under one cluster can process the request
            response = consumerStub.getInvokerCluster().invoke(request);
            return response.getResult();
        } catch (Exception e) {
            return handleError(request, e);
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

    private Responseable handleError(Requestable request, Exception cause) {
        if (ExceptionUtils.isBizException(cause)) {
            // Throw the exception if it is business one
            throw (RuntimeException) cause;
        }

        boolean throwException = consumerStub.getUrl().getBooleanOption(THROW_EXCEPTION, THROW_EXCEPTION_VAL_DEFAULT);
        if (throwException) {
            if (cause instanceof RpcAbstractException) {
                throw (RpcAbstractException) cause;
            } else {
                throw new RpcFrameworkException("Failed to call the request with error", cause);
            }
        }
        return RpcResponse.error(request, cause);
    }
}
