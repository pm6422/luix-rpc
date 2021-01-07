package org.infinity.rpc.core.exchange.response;

public interface RpcResponseFuture extends Responseable, Future {
    void onSuccess(Responseable response);

    void onFailure(Responseable response);

    long getCreateTime();

    void setReturnType(Class<?> clazz);
}
