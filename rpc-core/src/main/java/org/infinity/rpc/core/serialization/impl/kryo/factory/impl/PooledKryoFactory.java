package org.infinity.rpc.core.serialization.impl.kryo.factory.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.infinity.rpc.core.serialization.impl.kryo.factory.AbstractKryoFactory;

public class PooledKryoFactory extends AbstractKryoFactory {

    private KryoPool pool;

    public PooledKryoFactory() {
        // Build pool with SoftReferences enabled (optional)
        pool = new KryoPool.Builder(this).softReferences().build();
    }

    @Override
    public Kryo getKryo() {
        return pool.borrow();
    }

    @Override
    public void returnKryo(Kryo kryo) {
        pool.release(kryo);
    }
}