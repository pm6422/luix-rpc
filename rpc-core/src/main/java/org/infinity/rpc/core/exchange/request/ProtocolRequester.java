package org.infinity.rpc.core.exchange.request;

import org.infinity.rpc.core.exchange.RpcCallable;
import org.infinity.rpc.core.url.Url;

/**
 * The initiator of the RPC request
 * It used to call the RPC provider
 * One requester for a protocol
 */
public interface ProtocolRequester<T> extends RpcCallable<T> {

    Url getProviderUrl();

}
