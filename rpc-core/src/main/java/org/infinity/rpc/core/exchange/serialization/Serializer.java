package org.infinity.rpc.core.exchange.serialization;

import org.infinity.rpc.utilities.spi.ServiceLoader;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.io.IOException;

@Spi(scope = SpiScope.SINGLETON)
public interface Serializer {

    /**
     * Serialize input object to byte array
     *
     * @param inputObject input object
     * @return byte array
     * @throws IOException if any IOException thrown
     */
    byte[] serialize(Object inputObject) throws IOException;

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

    byte[] serializeMulti(Object[] data) throws IOException;

    Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException;

    /**
     * serialization的唯一编号，用于传输协议中指定序列化方式。每种序列化的编号必须唯一。
     *
     * @return 由于编码规范限制，序列化方式最大支持32种，因此返回值必须在0-31之间。
     */
    int getSerializationTypeNum();

    /**
     * Get serializer instance associated with the specified name
     *
     * @param name specified serializer name
     * @return serializer instance
     */
    static Serializer getInstance(String name) {
        return ServiceLoader.forClass(Serializer.class).load(name);
    }
}