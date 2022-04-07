package org.infinity.luix.utilities.serializer;

import org.infinity.luix.utilities.serviceloader.ServiceLoader;
import org.infinity.luix.utilities.serviceloader.annotation.Spi;
import org.infinity.luix.utilities.serviceloader.annotation.SpiScope;

import java.io.IOException;

@Spi(scope = SpiScope.SINGLETON)
public interface Serializer {
    /**
     * kryo, hessian2, grpc-pb, json, msgpack, hprose, protobuf, simple, grpc-pb-json
     */
    int    SERIALIZER_ID_KRYO       = 1;
    int    SERIALIZER_ID_HESSIAN2   = 2;
    String SERIALIZER_NAME_KRYO     = "kryo";
    String SERIALIZER_NAME_HESSIAN2 = "hessian2";

    /**
     * Serialize input object to byte array
     *
     * @param object input object
     * @return byte array
     * @throws IOException if any IOException thrown
     */
    byte[] serialize(Object object) throws IOException;

    /**
     * Deserialize byte array to output object
     *
     * @param bytes            byte array
     * @param outputObjectType output object type
     * @param <T>              output object type generic
     * @return output object
     * @throws IOException if any IOException thrown
     */
    <T> T deserialize(byte[] bytes, Class<T> outputObjectType) throws IOException;

    /**
     * Serialize array
     *
     * @param objects object array
     * @return serialized bytes
     * @throws IOException if any exception throws
     */
    byte[] serializeArray(Object[] objects) throws IOException;

    /**
     * Deserialize object array
     *
     * @param data    serialized bytes
     * @param classes target object class
     * @return Object array
     * @throws IOException if any exception throws
     */
    Object[] deserializeArray(byte[] data, Class<?>[] classes) throws IOException;

    /**
     * Get serializer unique ID，it used to specify serializer in transport protocol
     *
     * @return a value in the range of 0-31
     */
    int getSerializerId();

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static Serializer getInstance(String name) {
        return ServiceLoader.forClass(Serializer.class).load(name);
    }
}