package org.infinity.rpc.core.client.invocationhandler;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.ratelimit.RateLimiter;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.client.request.impl.RpcRequest;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.core.utils.RpcConfigValidator;

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
        if (limitRate()) {
            log.warn("Rate limiting!");
            return null;
        }

        validate();

        // Set method arguments
        request.setMethodArguments(args);

        // Set some options
        request.addOption(FORM, consumerStub.getForm());
        request.addOption(VERSION, consumerStub.getVersion());
        request.addOption(HEALTH_CHECKER, consumerStub.getHealthChecker());
        request.addOption(REQUEST_TIMEOUT, consumerStub.getRequestTimeout(), REQUEST_TIMEOUT_VAL_DEFAULT);
        request.addOption(MAX_RETRIES, consumerStub.getMaxRetries(), MAX_RETRIES_VAL_DEFAULT);
        request.addOption(MAX_PAYLOAD, consumerStub.getMaxPayload(), MAX_PAYLOAD_VAL_DEFAULT);
        return sendRequest(request, returnType);
    }

    /**
     * Validate
     */
    protected void validate() {
        RpcConfigValidator.notNull(consumerStub.getServiceInvoker(), "Invoker cluster must NOT be null!");
    }

    /**
     * @param request    RPC request
     * @param returnType return type of method
     * @return result of method execution
     */
    protected Object sendRequest(Requestable request, Class<?> returnType) {
        Responseable response;
        try {
            // Call chain: service invoker call => cluster fault tolerance strategy =>
            // LB select node => provider invoker call
            response = consumerStub.getServiceInvoker().invoke(request);
            return response.getResult();
        } catch (Exception e) {
            return handleError(request, e);
        } finally {
        }
    }

    /**
     * Limit rate on client side
     * It is a temporary solution
     *
     * @return {@code true} if it need limit rate and {@code false} otherwise
     */
    private boolean limitRate() {
        return consumerStub.isLimitRate() && !RateLimiter.getInstance(RATE_LIMITER_GUAVA).tryAcquire();
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

    private Responseable handleError(Requestable request, Exception cause) {
        if (ExceptionUtils.isBizException(cause)) {
            // Throw the exception if it is business exception
            throw (RuntimeException) cause;
        }

        boolean throwException = consumerStub.getUrl().getBooleanOption(THROW_EXCEPTION, THROW_EXCEPTION_VAL_DEFAULT);
        if (throwException) {
            // Covert to runtime exception
            throw (RpcAbstractException) cause;
        }
        return RpcResponse.error(request, cause);
    }
}
