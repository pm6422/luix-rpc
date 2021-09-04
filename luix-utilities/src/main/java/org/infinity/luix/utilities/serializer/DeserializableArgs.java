package org.infinity.luix.utilities.serializer;


import java.io.IOException;

public class DeserializableArgs {
    private final Serializer serializer;
    private final byte[]     objBytes;

    public DeserializableArgs(Serializer serializer, byte[] argsBytes) {
        this.serializer = serializer;
        this.objBytes = argsBytes;
    }

    public Object[] deserialize(Class<?>[] paramTypes) throws IOException {
        Object[] ret = null;
        if (paramTypes != null && paramTypes.length > 0) {
            ret = serializer.deserializeArray(objBytes, paramTypes);
        }
        return ret;
    }
}
