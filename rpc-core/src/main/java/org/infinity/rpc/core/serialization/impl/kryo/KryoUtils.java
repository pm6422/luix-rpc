package org.infinity.rpc.core.serialization.impl.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import org.infinity.rpc.core.serialization.impl.kryo.factory.AbstractKryoFactory;
import org.infinity.rpc.core.serialization.impl.kryo.factory.impl.PooledKryoFactory;

public abstract class KryoUtils {
    private static final AbstractKryoFactory KRYO_FACTORY = new PooledKryoFactory();

    /**
     * Get a new {@link Kryo} instance for each thread
     *
     * @return kryo instance
     */
    public static Kryo get() {
        return KRYO_FACTORY.getKryo();
    }

    public static void register(Class<?> clazz) {
        KRYO_FACTORY.registerClass(clazz);
    }

    /**
     * only supposed to be called at startup time
     *
     * @param clazz      object type
     * @param serializer object serializer
     */
    public static void registerClass(Class<?> clazz, Serializer serializer) {
        KRYO_FACTORY.registerClass(clazz, serializer);
    }

    public static void setRegistrationRequired(boolean registrationRequired) {
        KRYO_FACTORY.setRegistrationRequired(registrationRequired);
    }

    public static void release(Kryo kryo) {
        KRYO_FACTORY.returnKryo(kryo);
    }
}