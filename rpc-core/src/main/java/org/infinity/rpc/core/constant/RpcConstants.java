package org.infinity.rpc.core.constant;

public class RpcConstants {
    public static final byte   FLAG_REQUEST             = 0x00;
    public static final byte   FLAG_RESPONSE            = 0x01;
    public static final byte   FLAG_RESPONSE_VOID       = 0x03;
    public static final byte   FLAG_RESPONSE_EXCEPTION  = 0x05;
    public static final byte   FLAG_RESPONSE_ATTACHMENT = 0x07;
    /**
     * suffix for async call
     */
    public static final String ASYNC_SUFFIX             = "Async";
}
