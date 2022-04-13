package com.luixtech.rpc.core.exchange.client;

public interface SharedObjectFactory<T> {

    /**
     * Build object
     *
     * @return object
     */
    T buildObject();

    /**
     * Rebuild object
     *
     * @param obj   object
     * @param async async flag
     * @return {@code true} if it was built and {@code false} otherwise
     */
    boolean rebuildObject(T obj, boolean async);

}
