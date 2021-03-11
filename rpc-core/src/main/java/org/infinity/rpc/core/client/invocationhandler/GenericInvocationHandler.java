package org.infinity.rpc.core.client.invocationhandler;

import java.util.Map;

public interface GenericInvocationHandler {
    /**
     * Generic invoke without dependencies of service provider API
     *
     * @param methodName       method name
     * @param methodParamTypes method parameter types string
     * @param args             method arguments
     * @param options          request options
     * @return return object
     */
    Object genericInvoke(String methodName, String[] methodParamTypes, Object[] args, Map<String, String> options);
}
