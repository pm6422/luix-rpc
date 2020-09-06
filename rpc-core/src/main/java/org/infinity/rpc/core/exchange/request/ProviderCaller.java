package org.infinity.rpc.core.exchange.request;

import org.infinity.rpc.core.exchange.ProviderCallable;
import org.infinity.rpc.core.url.Url;

/**
 * The initiator of the RPC request
 * It used to call the RPC provider
 * One caller for a protocol
 *
 * @param <T>: The interface class of the provider
 */
public interface ProviderCaller<T> extends ProviderCallable<T> {

    Url getProviderUrl();

}
