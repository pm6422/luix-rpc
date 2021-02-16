package org.infinity.rpc.core.serialization;


import java.io.IOException;

public class DeserializableObject {
    private final Serializer serializer;
    private final byte[]     objBytes;

    public DeserializableObject(Serializer serializer, byte[] objBytes) {
        this.serializer = serializer;
        this.objBytes = objBytes;
    }

    public <T> T deserialize(Class<T> clz) throws IOException {
        return serializer.deserialize(objBytes, clz);
    }

    public Object[] deserializeMulti(Class<?>[] paramTypes) throws IOException {
        Object[] ret = null;
        if (paramTypes != null && paramTypes.length > 0) {
            ret = serializer.deserializeMulti(objBytes, paramTypes);
        }
        return ret;
    }
}
