package com.luixtech.rpc.serializer.kryo;

import com.luixtech.rpc.serializer.Serializer;
import com.luixtech.rpc.serializer.kryo.io.KryoObjectInput;
import com.luixtech.rpc.serializer.kryo.io.KryoObjectOutput;
import com.luixtech.utilities.serviceloader.annotation.SpiName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Kryo is a fast and efficient binary object graph serialization framework for Java.
 * Kryo can also perform automatic deep and shallow copying/cloning.
 * This is direct copying from object to object, not object to bytes to object.
 * Because Kryo is not thread safe and constructing and configuring a Kryo instance is relatively expensive,
 * in a multi-threaded environment ThreadLocal or pooling might be considered.
 * <p>
 * Kryo requirements: No
 * Supported languages: JAVA
 * <p>
 * Refer to https://github.com/EsotericSoftware/kryo
 */
@SpiName(Serializer.SERIALIZER_NAME_KRYO)
public class KryoSerializer implements Serializer {
    @Override
    public byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KryoObjectOutput out = new KryoObjectOutput(KryoUtils.get(), bos);
        out.writeObject(object);
        out.flush();
        return bos.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        if (data == null) {
            return null;
        }
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
        if (data == null) {
            return null;
        }
        KryoObjectInput input = new KryoObjectInput(KryoUtils.get(), new ByteArrayInputStream(data));
        Object[] objects = new Object[classes.length];
        for (int i = 0; i < classes.length; i++) {
            objects[i] = input.readObject(classes[i]);
        }
        return objects;
    }

    @Override
    public int getSerializerId() {
        return SERIALIZER_ID_KRYO;
    }
}
