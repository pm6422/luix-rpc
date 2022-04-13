package com.luixtech.luixrpc.core.client.invocationhandler.impl;

import com.luixtech.uidgenerator.core.id.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import com.luixtech.luixrpc.core.client.invocationhandler.AbstractConsumerInvocationHandler;
import com.luixtech.luixrpc.core.client.invocationhandler.UniversalInvocationHandler;
import com.luixtech.luixrpc.core.client.proxy.impl.JdkProxy;
import com.luixtech.luixrpc.core.client.request.impl.RpcRequest;
import com.luixtech.luixrpc.core.client.stub.ConsumerStub;
import com.luixtech.luixrpc.core.server.response.FutureResponse;
import com.luixtech.luixrpc.core.utils.MethodParameterUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @param <T>: The interface class of the consumer
 */
@Slf4j
public class ConsumerInvocationHandler<T> extends AbstractConsumerInvocationHandler<T>
        implements InvocationHandler, UniversalInvocationHandler {

    public ConsumerInvocationHandler(ConsumerStub<T> consumerStub) {
        super.consumerStub = consumerStub;
    }

    /**
     * Invoke this method every time when all the methods of RPC consumer been invoked
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
            return JdkProxy.class.getSimpleName();
        }

        // Create a new RpcRequest for each request
        RpcRequest request = new RpcRequest(IdGenerator.generateTimestampId(),
                consumerStub.getProtocol(),
                consumerStub.getInterfaceName(),
                method.getName(),
                MethodParameterUtils.getMethodParameters(method),
                isAsyncMethod(method));
        return process(request, args);
    }

    @Override
    public Object invoke(String methodName) {
        return invoke(methodName, new String[]{}, null);
    }

    @Override
    public Object invoke(String methodName, String[] methodParamTypes, Object[] args) {
        // Create a new RpcRequest for each request
        RpcRequest request = new RpcRequest(IdGenerator.generateTimestampId(),
                consumerStub.getProtocol(),
                consumerStub.getInterfaceName(),
                methodName,
                ArrayUtils.isEmpty(methodParamTypes) ? MethodParameterUtils.VOID : String.join(MethodParameterUtils.PARAM_TYPE_STR_DELIMITER, methodParamTypes),
                false);
        return process(request, args);
    }

    /**
     * It is a asynchronous method calling if the return type of method is type of {@link FutureResponse}
     *
     * @param method method
     * @return {@code true} if it was async call and {@code false} otherwise
     */
    private boolean isAsyncMethod(Method method) {
        return method.getReturnType().equals(FutureResponse.class);
    }
}
