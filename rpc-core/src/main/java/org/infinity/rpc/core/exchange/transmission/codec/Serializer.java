package org.infinity.rpc.core.exchange.transmission.codec;

import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.io.IOException;

@Spi(scope = SpiScope.SINGLETON)
public interface Serializer {

    byte[] serialize(Object obj) throws IOException;

    <T> T deserialize(byte[] bytes, Class<T> clz) throws IOException;

    byte[] serializeMulti(Object[] data) throws IOException;

    Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException;

    /**
     * serialization的唯一编号，用于传输协议中指定序列化方式。每种序列化的编号必须唯一。
     *
     * @return 由于编码规范限制，序列化方式最大支持32种，因此返回值必须在0-31之间。
     */
    int getSerializationNumber();
}