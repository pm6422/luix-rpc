package org.infinity.rpc.core.client.invocationhandler.impl;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.invocationhandler.AbstractConsumerInvocationHandler;
import org.infinity.rpc.core.client.proxy.ConsumerProxy;
import org.infinity.rpc.core.client.request.impl.RpcRequest;
import org.infinity.rpc.core.client.stub.ConsumerStub;
import org.infinity.rpc.utilities.annotation.Asynchronized;
import org.infinity.rpc.utilities.id.IdGenerator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static org.infinity.rpc.core.constant.ServiceConstants.*;
import static org.infinity.rpc.core.utils.MethodParameterUtils.getMethodParameters;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public class ConsumerInvocationHandler<T> extends AbstractConsumerInvocationHandler<T> implements InvocationHandler {

    public ConsumerInvocationHandler(ConsumerStub<T> stub) {
        consumerStub = stub;
    }

    /**
     * Call this method every time when all the methods of RPC consumer been invoked
     *
     * @param proxy  consumer proxy instance
     * @param method consumer method
     * @param args   consumer method arguments
     * @return RPC invocation result
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (isDerivedFromObject(method)) {
            // IDE may call the Object.toString() method if you set some break pointers.
            return ConsumerProxy.class.getSimpleName();
        }

        // Create a new RpcRequest for each request
        RpcRequest request = new RpcRequest(IdGenerator.generateTimestampId(),
                consumerStub.getInterfaceName(),
                method.getName(),
                getMethodParameters(method));

        // Set method arguments
        request.setMethodArguments(args);

        // Set some options
        request.addOption(GROUP, consumerStub.getGroup());
        request.addOption(VERSION, consumerStub.getVersion());
        request.addOption(CHECK_HEALTH_FACTORY, consumerStub.getCheckHealthFactory());
        request.addOption(REQUEST_TIMEOUT, String.valueOf(consumerStub.getRequestTimeout()));
        request.addOption(MAX_RETRIES, String.valueOf(consumerStub.getMaxRetries()));

        return processRequest(request, method.getReturnType(), isAsyncMethod(method));
    }

    /**
     * Check whether the method is derived from {@link Object} class.
     * e.g, toString, equals, hashCode, finalize
     *
     * @param method method
     * @return true: method derived from Object class, false: otherwise
     */
    public boolean isDerivedFromObject(Method method) {
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

    /**
     * It is a asynchronous method calling if the method is annotated with {@link Asynchronized}
     *
     * @param method method
     * @return true: async call, false: sync call
     */
    private boolean isAsyncMethod(Method method) {
        return method.isAnnotationPresent(Asynchronized.class);
    }
}
