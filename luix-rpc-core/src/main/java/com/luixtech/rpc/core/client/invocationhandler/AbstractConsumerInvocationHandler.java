package com.luixtech.rpc.core.client.invocationhandler;

import com.luixtech.rpc.core.client.ratelimit.RateLimiter;
import com.luixtech.rpc.core.client.request.Requestable;
import com.luixtech.rpc.core.client.request.impl.RpcRequest;
import com.luixtech.rpc.core.client.stub.ConsumerStub;
import com.luixtech.rpc.core.constant.ConsumerConstants;
import com.luixtech.rpc.core.constant.ProtocolConstants;
import com.luixtech.rpc.core.constant.RpcConstants;
import com.luixtech.rpc.core.constant.ServiceConstants;
import com.luixtech.rpc.core.exception.ExceptionUtils;
import com.luixtech.rpc.core.exception.RpcAbstractException;
import com.luixtech.rpc.core.exception.impl.RpcInvocationException;
import com.luixtech.rpc.core.server.response.Responseable;
import com.luixtech.rpc.core.server.response.impl.RpcResponse;
import com.luixtech.rpc.core.utils.MethodParameterUtils;
import com.luixtech.rpc.core.utils.RpcConfigValidator;
import com.luixtech.rpc.metrics.MetricsUtils;
import com.luixtech.rpc.serializer.DeserializableResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Method;

import static com.luixtech.rpc.core.utils.RpcFrameworkUtils.getMethodKey;
import static com.luixtech.rpc.metrics.ResponseType.BIZ_EXCEPTION;
import static com.luixtech.rpc.metrics.ResponseType.NORMAL;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public abstract class AbstractConsumerInvocationHandler<T> {
    protected ConsumerStub<T> consumerStub;

    /**
     * @param request RPC request
     * @param args    method arguments
     * @return result of method
     */
    protected Object process(RpcRequest request, Object[] args) {
        if (limitRate()) {
            log.warn("Rate limiting!");
            return null;
        }

        validate(request, args);

        // Set method arguments
        request.setMethodArguments(args);

        // Set some options
        request.addOption(ProtocolConstants.SERIALIZER, consumerStub.getSerializer());
        request.addOption(ServiceConstants.FORM, consumerStub.getForm());
        request.addOption(ServiceConstants.VERSION, consumerStub.getVersion());
        request.addOption(ServiceConstants.REQUEST_TIMEOUT, consumerStub.getRequestTimeout());
        request.addOption(ServiceConstants.RETRY_COUNT, consumerStub.getRetryCount());
        request.addOption(ServiceConstants.MAX_PAYLOAD, consumerStub.getMaxPayload());
        Object result = sendRequest(request);
        result = processResult(result);
        return result;
    }

    /**
     * Validate
     */
    protected void validate(RpcRequest request, Object[] args) {
        RpcConfigValidator.notNull(consumerStub.getInvokerInstance(), "Service invoker must NOT be null!");

        // Validate arguments
        if (!MethodParameterUtils.VOID.equals(request.getMethodParameters())) {
            Validate.isTrue(ArrayUtils.isNotEmpty(args), "The number of arguments must be the same as the number of parameters!");
        } else {
            Validate.isTrue(ArrayUtils.isEmpty(args), "The number of arguments must be the same as the number of parameters!");
        }
    }

    /**
     * @param request RPC request
     * @return result of method execution
     */
    protected Object sendRequest(Requestable request) {
        Responseable response = null;
        // todo: add filters
        long start = System.currentTimeMillis();
        try {
            response = consumerStub.getInvokerInstance().invoke(request);
            return response == null ? null : response.getResult();
        } catch (Exception e) {
            return handleError(request, e);
        } finally {
            String methodKey = getMethodKey(request.getProtocol(), request.getInterfaceName(),
                    request.getMethodName(), request.getOption(ServiceConstants.FORM), request.getOption(ServiceConstants.VERSION));
            long end = System.currentTimeMillis();
            if (response != null) {
                MetricsUtils.trackCall(methodKey, request.getRequestId(), end - start,
                        response.getElapsedTime(), RpcConstants.SLOW_EXE_THRESHOLD, NORMAL);
            } else {
                MetricsUtils.trackCall(methodKey, request.getRequestId(), end - start,
                        end - start, RpcConstants.SLOW_EXE_THRESHOLD, BIZ_EXCEPTION);
            }
        }
    }


    /**
     * Limit rate on client side
     * It is a temporary solution
     *
     * @return {@code true} if it need limit rate and {@code false} otherwise
     */
    private boolean limitRate() {
        return consumerStub.isLimitRate() && !RateLimiter.getInstance(ConsumerConstants.RATE_LIMITER_GUAVA).tryAcquire();
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

        boolean throwException = consumerStub.getUrl().getBooleanOption(ProtocolConstants.THROW_EXCEPTION, ProtocolConstants.THROW_EXCEPTION_VAL_DEFAULT);
        if (throwException) {
            // Covert to runtime exception
            throw (RpcAbstractException) cause;
        }
        return RpcResponse.error(request, cause);
    }

    private Object processResult(Object result) {
        if (result != null && result instanceof DeserializableResult) {
            try {
                result = ((DeserializableResult) result).deserialize();
            } catch (Exception e) {
                throw new RpcInvocationException("Failed to deserialize return value!", e);
            }
        }
        return result;
    }
}
