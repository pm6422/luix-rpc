package org.infinity.rpc.core.server;

import lombok.Data;

import java.util.Map;

@Data
public class RpcProviderHolder {
    Map<String, Object> rpcProviderMap;
}
