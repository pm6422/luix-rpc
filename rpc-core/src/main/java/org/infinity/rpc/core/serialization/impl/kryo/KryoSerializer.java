package org.infinity.rpc.core.serialization.impl.kryo;

import org.infinity.rpc.core.serialization.Serializer;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.infinity.rpc.core.constant.ProtocolConstants.SERIALIZER_KRYO_TYPE_NUM;
import static org.infinity.rpc.core.constant.ProtocolConstants.SERIALIZER_VAL_KRYO;

/**
 * Kryo requirements:
 */
@SpiName(SERIALIZER_VAL_KRYO)
public class KryoSerializer implements Serializer {
    @Override
    public byte[] serialize(Object data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KryoObjectOutput out = new KryoObjectOutput(bos);
        out.writeObject(data);
        out.flushBuffer();
        return bos.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        KryoObjectInput input = new KryoObjectInput(new ByteArrayInputStream(data));
        return input.readObject(clz);
    }

    @Override
    public byte[] serializeArray(Object[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KryoObjectOutput out = new KryoObjectOutput(bos);
        for (Object obj : data) {
            out.writeObject(obj);
        }
        out.flushBuffer();
        return bos.toByteArray();
    }

    @Override
    public Object[] deserializeArray(byte[] data, Class<?>[] classes) throws IOException {
        KryoObjectInput input = new KryoObjectInput(new ByteArrayInputStream(data));
        Object[] objects = new Object[classes.length];
        for (int i = 0; i < classes.length; i++) {
            objects[i] = input.readObject(classes[i]);
        }
        return objects;
    }

    @Override
    public int getSerializationTypeNum() {
        return SERIALIZER_KRYO_TYPE_NUM;
    }
}
