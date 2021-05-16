package org.infinity.rpc.core.serialization;

import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.io.IOException;

@Spi(scope = SpiScope.SINGLETON)
public interface Serializer {

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
     * @throws IOException
     */
    byte[] serializeArray(Object[] objects) throws IOException;

    /**
     * Deserialize object array
     *
     * @param data    serialized bytes
     * @param classes target object class
     * @return Object array
     * @throws IOException
     */
    Object[] deserializeArray(byte[] data, Class<?>[] classes) throws IOException;

    /**
     * serialization的唯一编号，用于传输协议中指定序列化方式。每种序列化的编号必须唯一。
     *
     * @return 由于编码规范限制，序列化方式最大支持32种，因此返回值必须在0-31之间。
     */
    int getSerializationTypeNum();

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