package org.infinity.rpc.core.serialization.impl.kryo.factory.impl;

import com.esotericsoftware.kryo.Kryo;
import org.infinity.rpc.core.serialization.impl.kryo.factory.AbstractKryoFactory;

@Deprecated
public class ThreadLocalKryoFactory extends AbstractKryoFactory {

    /**
     * Create a new {@link Kryo} for each thread
     */
    private final ThreadLocal<Kryo> holder = ThreadLocal.withInitial(this::create);

    @Override
    public Kryo getKryo() {
        return holder.get();
    }

    @Override
    public void returnKryo(Kryo kryo) {
        // Leave blank intentionally
    }
}