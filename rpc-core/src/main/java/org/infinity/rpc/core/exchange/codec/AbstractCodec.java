package org.infinity.rpc.core.exchange.codec;


import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.serialization.Serializer;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractCodec implements Codec {

    protected static Map<Integer, String> serializations = new ConcurrentHashMap<>();

    protected void serialize(ObjectOutput output, Object inputObject, Serializer serializer) throws IOException {
        if (inputObject == null) {
            output.writeObject(null);
            return;
        }
        // Serialize the input object to byte array first, then write the byte array to output
        output.writeObject(serializer.serialize(inputObject));
    }

    protected Object deserialize(byte[] bytes, Class<?> outputObjectType, Serializer serializer) throws IOException {
        if (bytes == null) {
            return null;
        }
        return serializer.deserialize(bytes, outputObjectType);
    }

    public ObjectOutput createOutput(OutputStream outputStream) {
        try {
            return new ObjectOutputStream(outputStream);
        } catch (Exception e) {
            throw new RpcFrameworkException("Failed to create object output stream by " + this.getClass().getSimpleName(), e,
                    RpcErrorMsgConstant.FRAMEWORK_ENCODE_ERROR);
        }
    }

    public ObjectInput createInput(InputStream in) {
        try {
            return new ObjectInputStream(in);
        } catch (Exception e) {
            throw new RpcFrameworkException("Failed to create object input stream by " + this.getClass().getSimpleName(), e,
                    RpcErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }
    }
}
