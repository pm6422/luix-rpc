package org.infinity.rpc.core.exchange.response.impl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RpcHealthResponse extends RpcResponse {
    private static final long serialVersionUID = -3779389154729791142L;

    public RpcHealthResponse(long requestId, Object result) {
        super(requestId, result);
    }
}
