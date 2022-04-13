package com.luixtech.utilities.serializer;


import java.io.IOException;

public class DeserializableResult {
    private final Serializer serializer;
    private final byte[]     objBytes;
    private final Class<?>   actualReturnType;

    public DeserializableResult(Serializer serializer, byte[] resultBytes, Class<?> actualReturnType) {
        this.serializer = serializer;
        this.objBytes = resultBytes;
        this.actualReturnType = actualReturnType;
    }

    public Object deserialize() throws IOException {
        return serializer.deserialize(objBytes, actualReturnType);
    }
}
