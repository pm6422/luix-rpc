package org.infinity.rpc.core.serialization.impl.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.KryoObjectInput;

import java.io.IOException;
import java.io.InputStream;

/**
 * It can read object by specified type and clean up the kryo instance
 */
public class KryoSpecifiedTypeObjectInput extends KryoObjectInput {

    private Kryo kryo;

    public KryoSpecifiedTypeObjectInput(Kryo kryo, InputStream inputStream) {
        super(kryo, new Input(inputStream));
        this.kryo = kryo;
    }

    public <T> T readObject(Class<T> clazz) {
        return kryo.readObjectOrNull(input, clazz);
    }

    public byte[] readBytes() throws IOException {
        try {
            int len = input.readInt();
            if (len < 0) {
                return null;
            } else if (len == 0) {
                return new byte[]{};
            } else {
                return input.readBytes(len);
            }
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public void cleanup() {
        KryoUtils.release(kryo);
        kryo = null;
    }
}