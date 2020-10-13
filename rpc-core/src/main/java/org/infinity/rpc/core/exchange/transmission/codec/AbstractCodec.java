package org.infinity.rpc.core.exchange.transmission.codec;


import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractCodec implements Codec {
    protected static ConcurrentHashMap<Integer, String> serializations = new ConcurrentHashMap<>();

    protected void serialize(ObjectOutput output, Object message, Serializer serialize) throws IOException {
        if (message == null) {
            output.writeObject(null);
            // return if null
            return;
        }
        output.writeObject(serialize.serialize(message));
    }

    protected Object deserialize(byte[] value, Class<?> type, Serializer serialize) throws IOException {
        if (value == null) {
            // return if null
            return null;
        }
        return serialize.deserialize(value, type);
    }

    public ObjectInput createInput(InputStream in) {
        try {
            return new ObjectInputStream(in);
        } catch (Exception e) {
            throw new RpcFrameworkException("Failed to create object input with " + this.getClass().getSimpleName(), e,
                    RpcErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }
    }

    public ObjectOutput createOutput(OutputStream outputStream) {
        try {
            return new ObjectOutputStream(outputStream);
        } catch (Exception e) {
            throw new RpcFrameworkException("Failed to create object output with " + this.getClass().getSimpleName(), e,
                    RpcErrorMsgConstant.FRAMEWORK_ENCODE_ERROR);
        }
    }
}
