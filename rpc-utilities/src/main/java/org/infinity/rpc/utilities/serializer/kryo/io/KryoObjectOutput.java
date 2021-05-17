package org.infinity.rpc.utilities.serializer.kryo.io;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.infinity.rpc.utilities.serializer.kryo.KryoUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * It can write object with specified type and clean up the kryo instance
 */
public class KryoObjectOutput extends com.esotericsoftware.kryo.io.KryoObjectOutput {

    private Kryo kryo;

    public KryoObjectOutput(Kryo kryo, OutputStream outputStream) {
        super(kryo, new Output(outputStream));
        this.kryo = kryo;
    }

    @Override
    public void writeObject(Object object) throws IOException {
        kryo.writeObjectOrNull(output, object, object.getClass());
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

    public void cleanup() {
        KryoUtils.release(kryo);
        kryo = null;
    }
}