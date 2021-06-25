package org.infinity.rpc.core.client.invocationhandler;

/**
 * If there is no API interface(e.g, jar) and model class on the server side.
 * All POJOs in the parameters and return values are represented by the Map and are usually used for framework integration.
 * For example, to implement a universal remote service testing system, handle all service requests
 * by implementing the {@link UniversalInvocationHandler} interface.
 */
public interface UniversalInvocationHandler {
    /**
     * Universal RPC invocation
     *
     * @param methodName method name
     * @return return object
     */
    Object invoke(String methodName);

    /**
     * Universal RPC invocation
     *
     * @param methodName       method name
     * @param methodParamTypes method parameter types string
     * @param args             method arguments
     * @return return object
     */
    Object invoke(String methodName, String[] methodParamTypes, Object[] args);
}
