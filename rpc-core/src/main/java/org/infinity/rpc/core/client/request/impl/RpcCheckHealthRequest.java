package org.infinity.rpc.core.client.request.impl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.utils.MethodParameterUtils;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class RpcCheckHealthRequest extends RpcRequest {
    private static final long   serialVersionUID            = 6503025793982931094L;
    public static final  String CHECK_HEALTH_INTERFACE_NAME = "org.infinity.rpc.health.Checker";
    public static final  String CHECK_HEALTH_METHOD_NAME    = "checkHealth";
    public static final  String CHECK_HEALTH_METHOD_PARAM   = MethodParameterUtils.VOID;

    public RpcCheckHealthRequest(long requestId) {
        this.requestId = requestId;
        this.interfaceName = CHECK_HEALTH_INTERFACE_NAME;
        this.methodName = CHECK_HEALTH_METHOD_NAME;
        this.methodParameters = CHECK_HEALTH_METHOD_PARAM;
        // Asynchronous call
        this.async = true;
    }

    public static boolean isCheckHealthRequest(Object message) {
        if (!(message instanceof Requestable)) {
            return false;
        }
        if (message instanceof RpcCheckHealthRequest) {
            return true;
        }
        Requestable request = (Requestable) message;
        return CHECK_HEALTH_INTERFACE_NAME.equals(request.getInterfaceName())
                && CHECK_HEALTH_METHOD_NAME.equals(request.getMethodName())
                && CHECK_HEALTH_METHOD_PARAM.endsWith(request.getMethodParameters());
    }
}
