package org.infinity.rpc.core.server.response.impl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RpcCheckHealthResponse extends RpcResponse {
    private static final long   serialVersionUID      = -3779389154729791142L;
    public static final  String CHECK_HEALTH_RESPONSE = "SUCCESS";

    public static RpcCheckHealthResponse of(long requestId) {
        RpcCheckHealthResponse response = new RpcCheckHealthResponse();
        response.setRequestId(requestId);
        response.setResultObject(CHECK_HEALTH_RESPONSE);
        return response;
    }

}
