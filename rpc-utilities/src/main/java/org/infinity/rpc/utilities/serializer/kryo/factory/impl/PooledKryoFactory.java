package org.infinity.rpc.utilities.serializer.kryo.factory.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;
import org.infinity.rpc.utilities.serializer.kryo.factory.AbstractKryoFactory;

public class PooledKryoFactory extends AbstractKryoFactory {

    /**
     * Build a thread-safe kryo pool
     */
    private final Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, 64) {
        @Override
        protected Kryo create() {
            return createInstance();
        }
    };

    @Override
    public Kryo createInstance() {
        return super.createInstance();
    }

    /**
     * Returns an object from this pool. The object may be new or reused one
     *
     * @return kryo instance
     */
    @Override
    public Kryo getKryo() {
        return kryoPool.obtain();
    }

    /**
     * Reset the kryo instance for reuse later
     *
     * @param kryo kryo instance
     */
    @Override
    public void releaseKryo(Kryo kryo) {
        kryoPool.free(kryo);
    }
}