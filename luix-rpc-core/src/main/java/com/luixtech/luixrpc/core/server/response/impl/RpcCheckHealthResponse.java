package com.luixtech.luixrpc.core.server.response.impl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RpcCheckHealthResponse extends RpcResponse {
    private static final long   serialVersionUID = -3779389154729791142L;
    public static final  String STATUS_OK        = "OK";
    public static final  String STATUS_INACTIVE  = "INACTIVE";

    public static RpcCheckHealthResponse of(long requestId) {
        RpcCheckHealthResponse response = new RpcCheckHealthResponse();
        response.setRequestId(requestId);
        response.setResult(STATUS_OK);
        return response;
    }

}
