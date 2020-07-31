package org.infinity.rpc.core.client.proxy;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.request.impl.RpcRequest;
import org.infinity.rpc.utilities.id.IdGenerator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @param <T>
 */
@Slf4j
public class RpcConsumerInvocationHandler<T> extends AbstractRpcConsumerInvocationHandler<T> implements InvocationHandler {

    public RpcConsumerInvocationHandler(Class<T> interfaceClass, List<Cluster<T>> clusters) {
        super.interfaceClass = interfaceClass;
        super.interfaceName = interfaceClass.getName();
        super.clusters = clusters;
    }

    /**
     * Call this method every time when all the methods of RPC consumer been invoked
     *
     * @param proxy
     * @param method consumer method
     * @param args   consumer method arguments
     * @return RPC invocation result
     * @throws Throwable if any exception throws
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isDerivedFromObject(method)) {

        }

        RpcRequest request = RpcRequest.builder()
                .requestId(IdGenerator.generateTimestampId())
                .interfaceName(interfaceName)
                .methodName(method.getName())
                .methodArguments(args)
                .build();

        boolean async = Arrays.asList(args).stream().anyMatch(arg -> (arg instanceof AsyncRequestFlag) && (AsyncRequestFlag.ASYNC.equals(arg)));
        Class returnType = getRealReturnType(async, this.clazz, method, method.getName());
        return processRequest(request, returnType, async);
    }

    /**
     * Check whether the method is derived from {@link Object} class.
     * For example, toString, equals, hashCode, finalize
     *
     * @param method method
     * @return true: method derived from Object class, false: otherwise
     */
    public boolean isDerivedFromObject(Method method) {
        if (method.getDeclaringClass().equals(Object.class)) {
            try {
                clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
                return false;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    private Class<?> getRealReturnType(boolean asyncCall, Class<?> clazz, Method method, String methodName) {
        if (asyncCall) {
            try {
                Method m = clazz.getMethod(methodName, method.getParameterTypes());
                return m.getReturnType();
            } catch (Exception e) {
                log.warn("RefererInvocationHandler get real return type fail. err:" + e.getMessage());
                return method.getReturnType();
            }
        } else {
            return method.getReturnType();
        }
    }
}
