package org.infinity.rpc.core.exchange.codec;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.Exchangable;
import org.infinity.rpc.core.exchange.codec.impl.DefaultCodec;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.transport.Channel;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;
import org.infinity.rpc.utilities.lang.ByteUtils;

import java.io.IOException;

@Slf4j
public class CodecUtils {
    public static byte[] encodeObjectToBytes(Channel channel, Codec codec, Exchangable msg) {
        try {
            byte[] data = encodeMessage(channel, codec, msg);
            short type = ByteUtils.bytes2short(data, 0);
            if (type == DefaultCodec.MAGIC) {
                return encodeV1(msg, data);
            }
//            else if (type == MotanV2Header.MAGIC) {
//                return data;
//            }
            else {
                throw new RpcFrameworkException("can not encode message, unknown magic:" + type);
            }
        } catch (IOException e) {
            throw new RpcFrameworkException("encode error: isResponse=" + (msg instanceof Responseable), e, RpcErrorMsgConstant.FRAMEWORK_ENCODE_ERROR);
        }
    }

    private static byte[] encodeV1(Object msg, byte[] data) throws IOException {
        long requestId = getRequestId(msg);
        byte[] result = new byte[RpcConstants.NETTY_HEADER + data.length];
        ByteUtils.short2bytes(RpcConstants.NETTY_MAGIC_TYPE, result, 0);
        result[3] = getType(msg);
        ByteUtils.long2bytes(requestId, result, 4);
        ByteUtils.int2bytes(data.length, result, 12);
        System.arraycopy(data, 0, result, RpcConstants.NETTY_HEADER, data.length);
        return result;
    }

    private static byte[] encodeMessage(Channel channel, Codec codec, Exchangable msg) throws IOException {
        byte[] data;
        if (msg instanceof Responseable) {
            try {
                data = codec.encode(channel, msg);
            } catch (Exception e) {
                log.error("NettyEncoder encode error, identity=" + channel.getProviderUrl().getIdentity(), e);
                Responseable oriResponse = (Responseable) msg;
                Responseable response = RpcFrameworkUtils.buildErrorResponse(oriResponse.getRequestId(), oriResponse.getProtocolVersion(), e);
                data = codec.encode(channel, response);
            }
        } else {
            data = codec.encode(channel, msg);
        }
        if (msg instanceof Requestable) {
            RpcFrameworkUtils.logEvent((Requestable) msg, RpcConstants.TRACE_CENCODE);
        } else if (msg instanceof Responseable) {
            RpcFrameworkUtils.logEvent((Responseable) msg, RpcConstants.TRACE_SENCODE);
        }
        return data;
    }

    private static long getRequestId(Object message) {
        if (message instanceof Requestable) {
            return ((Requestable) message).getRequestId();
        } else if (message instanceof Responseable) {
            return ((Responseable) message).getRequestId();
        } else {
            return 0;
        }
    }

    private static byte getType(Object message) {
        if (message instanceof Requestable) {
            return RpcConstants.FLAG_REQUEST;
        } else if (message instanceof Responseable) {
            return RpcConstants.FLAG_RESPONSE;
        } else {
            return RpcConstants.FLAG_OTHER;
        }
    }
}
