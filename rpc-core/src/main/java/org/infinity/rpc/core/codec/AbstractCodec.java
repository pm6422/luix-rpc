package org.infinity.rpc.core.codec;


import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.exception.impl.RpcServiceException;
import org.infinity.rpc.utilities.serializer.Serializer;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.exception.RpcErrorMsgConstant.ENCODE_ERROR;

public abstract class AbstractCodec implements Codec {

    protected static Map<Integer, String> serializations = new ConcurrentHashMap<>();

    protected void checkMessagePayloadSize(int actualPayloadSize, int maxPayloadSize) {
        if (actualPayloadSize > maxPayloadSize) {
            throw new RpcServiceException("The request data must NOT exceed the max message payload [" + maxPayloadSize + "]");
        }
    }

    /**
     * Serialize the input object to byte array first,
     * then write the byte array to output
     *
     * @param serializer  specified serializer
     * @param output      object output
     * @param inputObject input object
     * @throws IOException if any exception thrown
     */
    protected void serialize(Serializer serializer, ObjectOutput output, Object inputObject) throws IOException {
        if (inputObject == null) {
            output.writeObject(null);
            return;
        }
        output.writeObject(serializer.serialize(inputObject));
    }

    /**
     * Deserialize the byte array to output object based on output object type class
     *
     * @param bytes            byte array
     * @param outputObjectType output object type
     * @param serializer       specified serializer
     * @return output object
     * @throws IOException if any exception thrown
     */
    protected Object deserialize(byte[] bytes, Class<?> outputObjectType, Serializer serializer) throws IOException {
        if (bytes == null) {
            return null;
        }
        return serializer.deserialize(bytes, outputObjectType);
    }

    public ObjectOutput createOutputStream(OutputStream outputStream) {
        try {
            return new ObjectOutputStream(outputStream);
        } catch (Exception e) {
            throw new RpcFrameworkException("Failed to create output stream by " + this.getClass().getSimpleName(), e, ENCODE_ERROR);
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
