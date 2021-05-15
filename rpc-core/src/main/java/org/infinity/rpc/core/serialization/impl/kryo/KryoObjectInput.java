package org.infinity.rpc.core.serialization.impl.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import java.io.IOException;
import java.io.InputStream;

/**
 * Kryo object input implementation, kryo object can be clean
 */
public class KryoObjectInput {

    private       Kryo  kryo;
    private final Input input;

    public KryoObjectInput(InputStream inputStream) {
        input = new Input(inputStream);
        this.kryo = KryoUtils.get();
    }

    public boolean readBool() throws IOException {
        try {
            return input.readBoolean();
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public byte readByte() throws IOException {
        try {
            return input.readByte();
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public short readShort() throws IOException {
        try {
            return input.readShort();
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public int readInt() throws IOException {
        try {
            return input.readInt();
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public long readLong() throws IOException {
        try {
            return input.readLong();
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public float readFloat() throws IOException {
        try {
            return input.readFloat();
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public double readDouble() throws IOException {
        try {
            return input.readDouble();
        } catch (KryoException e) {
            throw new IOException(e);
        }
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

    public String readUtf() throws IOException {
        try {
            return input.readString();
        } catch (KryoException e) {
            throw new IOException(e);
        }
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        try {
            return kryo.readObjectOrNull(input, String.class);
        } catch (KryoException e) {
            throw new UnsupportedOperationException("Kryo serialization must know the input type when deserialize.", e);
        }
    }

    public Throwable readThrowable() throws IOException, ClassNotFoundException {
        return (Throwable) kryo.readClassAndObject(input);
    }

    public Object readEvent() throws IOException, ClassNotFoundException {
        return kryo.readObjectOrNull(input, String.class);
    }

    public <T> T readObject(Class<T> clazz) {
        return kryo.readObjectOrNull(input, clazz);
    }

    public void cleanup() {
        KryoUtils.release(kryo);
        kryo = null;
    }
}