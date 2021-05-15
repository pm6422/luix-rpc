package org.infinity.rpc.core.serialization.impl.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Kryo object output implementation, kryo object can be clean
 */
public class KryoObjectOutput {

    private final Output output;
    private       Kryo   kryo;

    public KryoObjectOutput(OutputStream outputStream) {
        output = new Output(outputStream);
        this.kryo = KryoUtils.get();
    }

    public void writeBool(boolean v) throws IOException {
        output.writeBoolean(v);
    }

    public void writeByte(byte v) throws IOException {
        output.writeByte(v);
    }

    public void writeShort(short v) throws IOException {
        output.writeShort(v);
    }

    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
        output.writeLong(v);
    }

    public void writeFloat(float v) throws IOException {
        output.writeFloat(v);
    }

    public void writeDouble(double v) throws IOException {
        output.writeDouble(v);
    }

    public void writeBytes(byte[] v) throws IOException {
        if (v == null) {
            output.writeInt(-1);
        } else {
            writeBytes(v, 0, v.length);
        }
    }

    public void writeBytes(byte[] v, int off, int len) throws IOException {
        if (v == null) {
            output.writeInt(-1);
        } else {
            output.writeInt(len);
            output.write(v, off, len);
        }
    }


    public void writeUtf(String v) throws IOException {
        output.writeString(v);
    }

    public void writeObject(Object v) throws IOException {
        kryo.writeObjectOrNull(output, v, v.getClass());
    }

    public void writeThrowable(Object v) throws IOException {
        kryo.writeClassAndObject(output, v);
    }

    public void flushBuffer() throws IOException {
        output.flush();
    }

    public void cleanup() {
        KryoUtils.release(kryo);
        kryo = null;
    }
}