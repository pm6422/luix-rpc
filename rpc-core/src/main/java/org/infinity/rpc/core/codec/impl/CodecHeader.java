package org.infinity.rpc.core.codec.impl;

import lombok.Data;
import org.infinity.rpc.core.exception.impl.RpcInvocationException;

import java.nio.ByteBuffer;

import static org.infinity.rpc.core.constant.ProtocolConstants.SERIALIZER_ID_DEFAULT;

@Data
public class CodecHeader {
    public static final short   MAGIC        = (short) 0xF1F1;
    /**
     * Header size
     */
    public static final int     HEADER_SIZE  = 13;
    /**
     * Protocol version
     */
//    private             int     version      = 1;
    /**
     * Check health indicator
     */
    private             boolean checkHealth  = false;
    /**
     * gzip indicator
     */
    private             boolean gzip         = false;
    /**
     * One way trip refers to only request but no response, round trip refers to both request and response
     */
    private             boolean oneWayTrip   = true;
    /**
     * 是否需要代理请求。motan agent使用
     */
    private             boolean proxy        = false;
    /**
     * Indicate whether it is a request, otherwise it's a response
     */
    private             boolean request      = true;
    /**
     * 消息状态。最大能表示8种状态，value range is 0 to 7. 0:normal, 1:exception
     * <p>
     * refer to {@link MessageStatus}
     */
    private             int     status       = 0;
    /**
     * Message body serializer，value range is 0 to 31.
     */
    private             int     serializerId = SERIALIZER_ID_DEFAULT;
    /**
     * Request ID
     */
    private             long    requestId;

    public byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(13);
        buf.putShort(MAGIC);
        byte msgType = (byte) 0x00;
        if (checkHealth) {
            msgType = (byte) (msgType | 0x10);
        }
        if (gzip) {
            msgType = (byte) (msgType | 0x08);
        }
        if (oneWayTrip) {
            msgType = (byte) (msgType | 0x04);
        }
        if (proxy) {
            msgType = (byte) (msgType | 0x02);
        }
        if (!request) {
            msgType = (byte) (msgType | 0x01);
        }

        buf.put(msgType);
        byte vs = 0x08;
//        if (version != 1) {
//            vs = (byte) ((version << 3) & 0xf8);
//        }
        if (status != 0) {
            vs = (byte) (vs | (status & 0x07));
        }
        buf.put(vs);
        byte se = 0x08;
        if (serializerId != 1) {
            se = (byte) ((serializerId << 3) & 0xf8);
        }
        buf.put(se);
        buf.putLong(requestId);
        buf.flip();
        return buf.array();
    }

    public static CodecHeader buildHeader(byte[] headerBytes) {
        ByteBuffer buf = ByteBuffer.wrap(headerBytes);
        short mg = buf.getShort();
        if (mg != MAGIC) {
            throw new RpcInvocationException("Found invalid magic: " + mg);
        }
        CodecHeader header = new CodecHeader();
        byte b = buf.get();
        if ((b & 0x10) == 0x10) {
            header.setCheckHealth(true);
        }
        if ((b & 0x08) == 0x08) {
            header.setGzip(true);
        }
        if ((b & 0x04) == 0x04) {
            header.setOneWayTrip(true);
        }
        if ((b & 0x02) == 0x02) {
            header.setProxy(true);
        }
        if ((b & 0x01) == 0x01) {
            header.setRequest(false);
        }

        b = buf.get();
//        header.setVersion((b >>> 3) & 0x1f);
        header.setStatus(b & 0x07);

        b = buf.get();
        header.setSerializerId((b >>> 3) & 0x1f);
        header.setRequestId(buf.getLong());

        return header;
    }

    /**
     * Message status
     */
    public enum MessageStatus {
        /**
         * Normal
         */
        NORMAL(0),
        /**
         * Exception
         */
        EXCEPTION(1);

        private final int status;

        MessageStatus(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
