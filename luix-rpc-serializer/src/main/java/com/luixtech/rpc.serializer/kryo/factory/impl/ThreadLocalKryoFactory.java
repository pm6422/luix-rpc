package com.luixtech.rpc.serializer.kryo.factory.impl;

import com.esotericsoftware.kryo.Kryo;
import com.luixtech.rpc.serializer.kryo.factory.AbstractKryoFactory;

public class ThreadLocalKryoFactory extends AbstractKryoFactory {

    /**
     * Create a new {@link Kryo} instance for each thread
     */
    private final ThreadLocal<Kryo> holder = ThreadLocal.withInitial(super::createInstance);

    @Override
    public Kryo getKryo() {
        return holder.get();
    }

    @Override
    public void releaseKryo(Kryo kryo) {
        holder.remove();
    }
}