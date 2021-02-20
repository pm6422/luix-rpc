package org.infinity.rpc.core.exchange.client;

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
     * @return true: built, false: not built
     */
    boolean rebuildObject(T obj, boolean async);

}
