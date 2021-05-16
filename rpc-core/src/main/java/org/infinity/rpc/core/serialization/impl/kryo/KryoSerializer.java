package org.infinity.rpc.core.serialization.impl.kryo;

import org.infinity.rpc.core.serialization.Serializer;
import org.infinity.rpc.core.serialization.impl.kryo.io.KryoObjectInput;
import org.infinity.rpc.core.serialization.impl.kryo.io.KryoObjectOutput;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.infinity.rpc.core.constant.ProtocolConstants.SERIALIZER_KRYO_TYPE_NUM;
import static org.infinity.rpc.core.constant.ProtocolConstants.SERIALIZER_VAL_KRYO;

/**
 * Kryo is a binary Java serialization library with a focus on doing it efficiently and automatically.
 * Because Kryo is not thread safe and constructing and configuring a Kryo instance is relatively expensive,
 * in a multi-threaded environment ThreadLocal or pooling might be considered.
 *
 * Refer to: https://github.com/EsotericSoftware/kryo
 */
@SpiName(SERIALIZER_VAL_KRYO)
public class KryoSerializer implements Serializer {
    @Override
    public byte[] serialize(Object data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KryoObjectOutput out = new KryoObjectOutput(KryoUtils.get(), bos);
        out.writeObject(data);
        out.flush();
        return bos.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        KryoObjectInput input = new KryoObjectInput(KryoUtils.get(), new ByteArrayInputStream(data));
        return input.readObject(clz);
    }

    @Override
    public byte[] serializeArray(Object[] objects) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KryoObjectOutput out = new KryoObjectOutput(KryoUtils.get(), bos);
        for (Object object : objects) {
            out.writeObject(object);
        }
        out.flush();
        return bos.toByteArray();
    }

    @Override
    public Object[] deserializeArray(byte[] data, Class<?>[] classes) throws IOException {
        KryoObjectInput input = new KryoObjectInput(KryoUtils.get(), new ByteArrayInputStream(data));
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
