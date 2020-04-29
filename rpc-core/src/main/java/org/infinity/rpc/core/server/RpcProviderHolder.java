package org.infinity.rpc.core.server;

import lombok.Data;

import java.util.Map;

@Deprecated
@Data
public class RpcProviderHolder {
    Map<String, Object> rpcProviderMap;
}
