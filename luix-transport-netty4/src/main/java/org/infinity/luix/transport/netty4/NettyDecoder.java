package org.infinity.luix.transport.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.codec.Codec;
import org.infinity.luix.core.codec.CodecUtils;
import org.infinity.luix.core.constant.RpcConstants;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.Channel;
import org.infinity.luix.core.protocol.constants.ProtocolVersion;
import org.infinity.luix.core.server.response.Responseable;
import org.infinity.luix.core.utils.RpcFrameworkUtils;

import java.util.List;

@Slf4j
public class NettyDecoder extends ByteToMessageDecoder {

    public static final String  DECODER          = "decoder";
    private             Codec   codec;
    private             Channel channel;
    private             int     maxContentLength = 0;

    public NettyDecoder(Codec codec, Channel channel, int maxContentLength) {
        this.codec = codec;
        this.channel = channel;
        this.maxContentLength = maxContentLength;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() <= RpcConstants.NETTY_HEADER) {
            return;
        }

        in.markReaderIndex();
        short type = in.readShort();
        if (type != RpcConstants.NETTY_MAGIC_TYPE) {
            in.resetReaderIndex();
            throw new RpcFrameworkException("NettyDecoder transport header not support, type: " + type);
        }
        in.skipBytes(1);
        int rpcVersion = (in.readByte() & 0xff) >>> 3;
        switch (rpcVersion) {
            case 0:
                decodeV1(ctx, in, out);
                break;
            case 1:
                decodeV2(ctx, in, out);
                break;
            default:
                decodeV2(ctx, in, out);
        }
    }

    private void decodeV2(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        long startTime = System.currentTimeMillis();
        in.resetReaderIndex();
        if (in.readableBytes() < 21) {
            return;
        }
        in.skipBytes(2);
        boolean isRequest = isV2Request(in.readByte());
        in.skipBytes(2);
        long requestId = in.readLong();
        int size = 13;
        int metaSize = in.readInt();
        size += 4;
        if (metaSize > 0) {
            size += metaSize;
            if (in.readableBytes() < metaSize) {
                in.resetReaderIndex();
                return;
            }
            in.skipBytes(metaSize);
        }
        if (in.readableBytes() < 4) {
            in.resetReaderIndex();
            return;
        }
        int bodySize = in.readInt();
        checkMaxContext(bodySize, ctx, isRequest, requestId, ProtocolVersion.VERSION_2);
        size += 4;
        if (bodySize > 0) {
            size += bodySize;
            if (in.readableBytes() < bodySize) {
                in.resetReaderIndex();
                return;
            }
        }
        byte[] data = new byte[size];
        in.resetReaderIndex();
        in.readBytes(data);
        decode(data, out, isRequest, requestId, ProtocolVersion.VERSION_2).setStartTime(startTime);
    }

    private boolean isV2Request(byte b) {
        return (b & 0x01) == 0x00;
    }

    private void decodeV1(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        long startTime = System.currentTimeMillis();
        in.resetReaderIndex();
        in.skipBytes(2);// skip magic num
        byte messageType = (byte) in.readShort();
        long requestId = in.readLong();
        int dataLength = in.readInt();

        // FIXME 如果dataLength过大，可能导致问题
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        checkMaxContext(dataLength, ctx, messageType == RpcConstants.FLAG_REQUEST, requestId, ProtocolVersion.VERSION_1);
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        decode(data, out, messageType == RpcConstants.FLAG_REQUEST, requestId, ProtocolVersion.VERSION_1).setStartTime(startTime);
    }

    private void checkMaxContext(int dataLength, ChannelHandlerContext ctx, boolean isRequest, long requestId, ProtocolVersion version) throws Exception {
        if (maxContentLength > 0 && dataLength > maxContentLength) {
            log.warn("NettyDecoder transport data content length over of limit, size: {}  > {}. remote={} local={}",
                    dataLength, maxContentLength, ctx.channel().remoteAddress(), ctx.channel().localAddress());
            Exception e = new RpcFrameworkException("NettyDecoder transport data content length over of limit, size: " + dataLength + " > " + maxContentLength);
            if (isRequest) {
                Responseable response = RpcFrameworkUtils.buildErrorResponse(requestId, version.getVersion(), e);
                byte[] msg = CodecUtils.encodeObjectToBytes(channel, codec, response);
                ctx.channel().writeAndFlush(msg);
                throw e;
            } else {
                throw e;
            }
        }
    }

    private NettyMessage decode(byte[] data, List<Object> out, boolean isRequest, long requestId, ProtocolVersion version) {
        NettyMessage message = new NettyMessage(isRequest, requestId, data, version);
        out.add(message);
        return message;
    }

}
