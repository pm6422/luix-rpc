package org.infinity.rpc.core.client.proxy;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.config.spring.config.InfinityProperties;
import org.infinity.rpc.core.exchange.cluster.Cluster;
import org.infinity.rpc.core.exchange.cluster.ClusterHolder;
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

    public RpcConsumerInvocationHandler(Class<T> interfaceClass, InfinityProperties infinityProperties) {
        super.interfaceClass = interfaceClass;
        super.interfaceName = interfaceClass.getName();
        super.infinityProperties = infinityProperties;
        List<Cluster<T>> clusters = ClusterHolder.getInstance().getClusters();
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

        boolean async = isAsyncCall(args);
        return processRequest(request, method.getReturnType(), async);
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
                interfaceClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
                return false;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    /**
     * It is a asynchronous request if any argument of the method is type of AsyncRequestFlag.ASYNC
     *
     * @param args method arguments
     * @return
     */
    private boolean isAsyncCall(Object[] args) {
        if (args == null) {
            return false;
        }
        return Arrays.asList(args)
                .stream()
                .anyMatch(arg -> (arg instanceof AsyncRequestFlag) && (AsyncRequestFlag.ASYNC.equals(arg)));
    }
}
