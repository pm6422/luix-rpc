package org.infinity.rpc.core.exchange.request.impl;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class RpcHealthRequest extends RpcRequest{
    private static final long serialVersionUID = 6503025793982931094L;

    public RpcHealthRequest(long requestId, String interfaceName, String methodName, String methodParameters) {
        this.requestId = requestId;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.methodParameters = methodParameters;
    }
}
