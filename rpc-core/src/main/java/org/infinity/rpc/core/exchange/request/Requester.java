package org.infinity.rpc.core.exchange.request;

import org.infinity.rpc.core.exchange.RpcCallable;
import org.infinity.rpc.core.url.Url;

/**
 * The initiator of the RPC request
 */
public interface Requester<T> extends RpcCallable<T> {

    Url getClientUrl();

}
