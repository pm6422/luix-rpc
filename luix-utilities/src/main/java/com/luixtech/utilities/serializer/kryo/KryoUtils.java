package com.luixtech.utilities.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.luixtech.utilities.serializer.kryo.factory.impl.ThreadLocalKryoFactory;
import com.luixtech.utilities.serializer.kryo.factory.AbstractKryoFactory;

public abstract class KryoUtils {

    private static final AbstractKryoFactory KRYO_FACTORY = new ThreadLocalKryoFactory();

    /**
     * Get or create a new {@link Kryo} instance for one thread
     *
     * @return kryo instance
     */
    public static Kryo get() {
        return KRYO_FACTORY.getKryo();
    }

    /**
     * Register the class with default serializer
     *
     * @param clazz class type
     */
    public static void register(Class<?> clazz) {
        KRYO_FACTORY.registerClass(clazz);
    }

    /**
     * Register the class with specified serializer
     *
     * @param clazz      class type
     * @param serializer serializer
     */
    public static void registerClass(Class<?> clazz, Serializer<?> serializer) {
        KRYO_FACTORY.registerClass(clazz, serializer);
    }

    /**
     * Release kryo instance
     *
     * @param kryo kryo instance
     */
    public static void release(Kryo kryo) {
        KRYO_FACTORY.releaseKryo(kryo);
    }
}