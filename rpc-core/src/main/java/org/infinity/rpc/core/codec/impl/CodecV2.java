package org.infinity.rpc.core.codec.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.infinity.rpc.core.client.request.impl.RpcRequest;
import org.infinity.rpc.core.codec.AbstractCodec;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.impl.RpcConfigException;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.exception.impl.RpcInvocationException;
import org.infinity.rpc.core.exchange.Channel;
import org.infinity.rpc.core.exchange.Exchangable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.utilities.lang.MathUtils;
import org.infinity.rpc.utilities.serializer.DeserializableObject;
import org.infinity.rpc.utilities.serializer.Serializer;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.infinity.rpc.core.codec.impl.CodecHeader.HEADER_SIZE;
import static org.infinity.rpc.core.constant.ProtocolConstants.*;
import static org.infinity.rpc.core.protocol.constants.ProtocolVersion.VERSION_2;
import static org.infinity.rpc.core.utils.SerializerHolder.getSerializerById;

@Slf4j
@SpiName(CODEC_VAL_V2)
public class CodecV2 extends AbstractCodec {
    private static final byte   MASK                 = 0x07;
    private static final String M2_PATH              = "M_p";
    private static final String M2_METHOD            = "M_m";
    private static final String M2_METHOD_PARAMETERS = "M_mp";
    private static final String M2_PROCESS_TIME      = "M_pt";
    private static final String M2_ERROR             = "M_e";
    /**
     * 调用方来源标识，同与application
     */
    private static final String M2_SOURCE            = "M_s";
    private static final String M2_MODULE            = "M_mdu";

    @Override
    public byte[] encode(Channel channel, Exchangable input) throws IOException {
        try {
            CodecHeader header = new CodecHeader();
            GrowableByteBuffer buf = new GrowableByteBuffer(4096);
            // Meta
            int index = HEADER_SIZE;
            buf.position(index);
            buf.putInt(0);

            // Body represents arguments bytes for request or results bytes for response
            byte[] body;
            if (input instanceof RpcRequest) {
                // Encode request
                RpcRequest request = (RpcRequest) input;
                String providerSerializer = channel.getProviderUrl().getOption(SERIALIZER, SERIALIZER_VAL_DEFAULT);
                // Consumer configuration over provider side
                String serializer = defaultIfEmpty(request.getOption(SERIALIZER), providerSerializer);
                body = encodeRequest(request, header, buf, serializer);
            } else {
                // Encode response
                body = encodeResponse((RpcResponse) input, header, buf);
            }

            buf.position(buf.position() - 1);
            int metaLength = buf.position() - index - 4;
            buf.putInt(index, metaLength);

            // Body
            if (body != null && body.length > 0) {
                // todo: gzip body
                buf.putInt(body.length);
                buf.put(body);
            } else {
                buf.putInt(0);
            }

            // Header
            int position = buf.position();
            buf.position(0);
            buf.put(header.toBytes());
            buf.position(position);
            buf.flip();
            byte[] result = new byte[buf.remaining()];
            buf.get(result);
            return result;
        } catch (Exception e) {
            if (ExceptionUtils.isRpcException(e)) {
                throw (RuntimeException) e;
            } else {
                throw new RpcFrameworkException("Failed to encode input object: " + input, e);
            }
        }
    }

    private byte[] encodeRequest(RpcRequest request, CodecHeader header, GrowableByteBuffer metaBuf, String serializerName) throws IOException {
        byte[] argsBytes = null;
        Serializer serializer = Serializer.getInstance(serializerName);
        if (serializer == null) {
            throw new RpcConfigException("Serializer [" + serializerName + "] does NOT exist, " +
                    "please check whether the correct dependency is in your class path!");
        }
        header.setSerializerId(serializer.getSerializerId());
        putString(metaBuf, M2_PATH);
        putString(metaBuf, request.getInterfaceName());
        putString(metaBuf, M2_METHOD);
        putString(metaBuf, request.getMethodName());
        if (request.getMethodParameters() != null) {
            putString(metaBuf, M2_METHOD_PARAMETERS);
            putString(metaBuf, request.getMethodParameters());
        }
        // Set options
        putMap(metaBuf, request.getOptions());
        header.setRequestId(request.getRequestId());
        if (request.getMethodArguments() != null) {
            // Serialize argument arrays to bytes
            argsBytes = serializer.serializeArray(request.getMethodArguments());
        }
        return argsBytes;
    }

    private byte[] encodeResponse(RpcResponse response, CodecHeader header, GrowableByteBuffer metaBuf) throws IOException {
        byte[] resultsBytes = null;
        Serializer serializer = getSerializerById(response.getSerializerId());
        header.setSerializerId(serializer.getSerializerId());

        putString(metaBuf, M2_PROCESS_TIME);
        putString(metaBuf, String.valueOf(response.getElapsedTime()));
        if (response.getException() != null) {
            putString(metaBuf, M2_ERROR);
            putString(metaBuf, org.apache.commons.lang3.exception.ExceptionUtils.getMessage(response.getException()));
            header.setStatus(CodecHeader.MessageStatus.EXCEPTION.getStatus());
        }
        putMap(metaBuf, response.getOptions());
        header.setRequestId(response.getRequestId());
        header.setRequest(false);
        if (response.getException() == null) {
            // Serialize results to bytes
            resultsBytes = serializer.serialize(response.getResult());
        }
        return resultsBytes;
    }

    @Override
    public Object decode(Channel channel, String remoteIp, byte[] data) throws IOException {
        CodecHeader header = CodecHeader.buildHeader(data);
        Map<String, String> metaMap = new HashMap<>();
        ByteBuffer buf = ByteBuffer.wrap(data);
        int metaSize = buf.getInt(HEADER_SIZE);
        int index = HEADER_SIZE + 4;
        if (metaSize > 0) {
            byte[] meta = new byte[metaSize];
            buf.position(index);
            buf.get(meta);
            metaMap = decodeMeta(meta);
            index += metaSize;
        }
        int bodySize = buf.getInt(index);
        index += 4;
        Object obj = null;
        if (bodySize > 0) {
            byte[] body = new byte[bodySize];
            buf.position(index);
            buf.get(body);
            // todo: ungzip
            // 默认自适应序列化
            Serializer serializer = getSerializerById(header.getSerializerId());
            obj = new DeserializableObject(serializer, body);
        }
        if (header.isRequest()) {
            // Decode request
            return decodeRequest(header, metaMap, obj);
        } else {
            // Decode response
            return decodeResponse(header, metaMap, obj);
        }
    }

    private Object decodeRequest(CodecHeader header, Map<String, String> metaMap, Object obj) {
        RpcRequest request = new RpcRequest();
        request.setRequestId(header.getRequestId());
        request.setInterfaceName(metaMap.remove(M2_PATH));
        request.setMethodName(metaMap.remove(M2_METHOD));
        request.setMethodParameters(metaMap.remove(M2_METHOD_PARAMETERS));
        request.setOptions(metaMap);
        // todo: check usage
        request.setProtocolVersion(VERSION_2.getVersion());
        request.setSerializerId(header.getSerializerId());
        if (obj != null) {
            request.setMethodArguments(new Object[]{obj});
        }
        return request;
    }

    private Object decodeResponse(CodecHeader header, Map<String, String> metaMap, Object result) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(header.getRequestId());
        response.setElapsedTime(MathUtils.parseLong(metaMap.remove(M2_PROCESS_TIME), 0));
        response.setOptions(metaMap);
        if (CodecHeader.MessageStatus.NORMAL.getStatus() == header.getStatus()) {
            // 解析正常消息
            response.setResult(result);
        } else {
            String errorMsg = metaMap.remove(M2_ERROR);
            log.error(errorMsg);
            Exception e = new RpcInvocationException(errorMsg);
            response.setException(e);
        }
        return response;
    }

    private void putString(GrowableByteBuffer buf, String content) {
        buf.put(content.getBytes(StandardCharsets.UTF_8));
        buf.put("\n".getBytes(StandardCharsets.UTF_8));
    }

    private void putMap(GrowableByteBuffer buf, Map<String, String> map) {
        if (MapUtils.isNotEmpty(map)) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                putString(buf, entry.getKey());
                putString(buf, entry.getValue());
            }
        }
    }

    private Map<String, String> decodeMeta(byte[] meta) {
        Map<String, String> map = new HashMap<>();
        if (ArrayUtils.isNotEmpty(meta)) {
            String[] s = new String(meta).split("\n");
            for (int i = 0; i < s.length - 1; i++) {
                map.put(s[i++], s[i]);
            }
        }
        return map;
    }
}
