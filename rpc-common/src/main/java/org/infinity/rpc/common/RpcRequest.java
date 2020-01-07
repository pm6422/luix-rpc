package org.infinity.rpc.common;

import lombok.ToString;

/**
 * Request class of RPC
 */
@ToString
public class RpcRequest {
    // 请求ID
    private String     requestId;
    // 请求类名(接口名称)
    private String     className;
    // 请求方法名称
    private String     methodName;
    // 请求方法参数类型列表
    private Class<?>[] parameterTypes;
    // 请求方法实参列表
    private Object[]   args;

    public RpcRequest(String requestId, String className, String methodName, Class<?>[] parameterTypes, Object[] args) {
        this.requestId = requestId;
        this.className = className;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.args = args;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Object[] getArgs() {
        return args;
    }
}
