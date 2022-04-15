package com.luixtech.rpc.core.codec.impl;

import com.luixtech.rpc.core.client.request.impl.RpcRequest;
import com.luixtech.rpc.core.codec.AbstractCodec;
import com.luixtech.rpc.core.constant.ProtocolConstants;
import com.luixtech.rpc.core.exception.ExceptionUtils;
import com.luixtech.rpc.core.exception.impl.RpcConfigException;
import com.luixtech.rpc.core.exception.impl.RpcFrameworkException;
import com.luixtech.rpc.core.exception.impl.RpcInvocationException;
import com.luixtech.rpc.core.exchange.Channel;
import com.luixtech.rpc.core.exchange.Exchangable;
import com.luixtech.rpc.core.protocol.constants.ProtocolVersion;
import com.luixtech.rpc.core.server.response.impl.RpcResponse;
import com.luixtech.rpc.core.utils.MethodParameterUtils;
import com.luixtech.rpc.core.utils.SerializerHolder;
import com.luixtech.rpc.serializer.DeserializableArgs;
import com.luixtech.rpc.serializer.DeserializableResult;
import com.luixtech.rpc.serializer.Serializer;
import com.luixtech.utilities.lang.MathUtils;
import com.luixtech.utilities.serviceloader.annotation.SpiName;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

@Slf4j
@SpiName(ProtocolConstants.CODEC_VAL_V2)
public class CodecV2 extends AbstractCodec {
    private static final String M_INTERFACE         = "M_i";
    private static final String M_METHOD            = "M_m";
    private static final String M_METHOD_PARAMETERS = "M_mp";
    private static final String M_RETURN_TYPE       = "M_rt";
    private static final String M_ELAPSED_TIME      = "M_et";
    private static final String M_ERROR             = "M_e";

    @Override
    public byte[] encode(Channel channel, Exchangable input) throws IOException {
        try {
            CodecHeader header = new CodecHeader();
            GrowableByteBuffer buf = new GrowableByteBuffer(4096);
            // Meta
            int index = CodecHeader.HEADER_SIZE;
            buf.position(index);
            buf.putInt(0);

            // Body represents arguments bytes for request or results bytes for response
            byte[] body;
            if (input instanceof RpcRequest) {
                // Encode request
                RpcRequest request = (RpcRequest) input;
                String providerSerializer = channel.getProviderUrl().getOption(ProtocolConstants.SERIALIZER, ProtocolConstants.SERIALIZER_VAL_DEFAULT);
                // Consumer configuration over provider side
                String serializer = defaultIfEmpty(request.getOption(ProtocolConstants.SERIALIZER), providerSerializer);
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
        Serializer serializer = Serializer.getInstance(serializerName);
        if (serializer == null) {
            throw new RpcConfigException("Serializer [" + serializerName + "] does NOT exist, " +
                    "please check whether the correct dependency is in your class path!");
        }
        // Set header
        header.setSerializerId(serializer.getSerializerId());
        header.setRequestId(request.getRequestId());

        // Set meta (including options)
        putString(metaBuf, M_INTERFACE);
        putString(metaBuf, request.getInterfaceName());

        putString(metaBuf, M_METHOD);
        putString(metaBuf, request.getMethodName());

        if (request.getMethodParameters() != null) {
            putString(metaBuf, M_METHOD_PARAMETERS);
            putString(metaBuf, request.getMethodParameters());
        }

        putMap(metaBuf, request.getOptions());

        // Serialize method arguments to bytes
        byte[] argsBytes = null;
        if (request.getMethodArguments() != null) {
            // Serialize argument arrays to bytes
            argsBytes = serializer.serializeArray(request.getMethodArguments());
        }
        return argsBytes;
    }

    private byte[] encodeResponse(RpcResponse response, CodecHeader header, GrowableByteBuffer metaBuf) throws IOException {
        Serializer serializer = SerializerHolder.getSerializerById(response.getSerializerId());

        // Set header
        header.setSerializerId(serializer.getSerializerId());
        header.setRequestId(response.getRequestId());
        header.setRequest(false);

        // Set meta
        // The actual return type is different from declared one.
        // e.g, the declared return type of interface class may by java.util.List,
        // but actual return type of implementation class may by java.util.ArrayList
        if (response.getResult() != null) {
            putString(metaBuf, M_RETURN_TYPE);
            putString(metaBuf, response.getResult().getClass().getName());
        }

        putString(metaBuf, M_ELAPSED_TIME);
        putString(metaBuf, String.valueOf(response.getElapsedTime()));

        if (response.getException() != null) {
            putString(metaBuf, M_ERROR);
            putString(metaBuf, org.apache.commons.lang3.exception.ExceptionUtils.getMessage(response.getException()));
            header.setStatus(CodecHeader.MessageStatus.EXCEPTION.getStatus());
        }
        putMap(metaBuf, response.getOptions());

        byte[] resultsBytes = null;
        if (response.getException() == null && response.getResult() != null) {
            // Serialize results to bytes
            resultsBytes = serializer.serialize(response.getResult());
        }
        return resultsBytes;
    }

    @Override
    public Object decode(Channel channel, String remoteIp, byte[] data) throws IOException, ClassNotFoundException {
        CodecHeader header = CodecHeader.buildHeader(data);
        Map<String, String> metaMap = new HashMap<>();
        ByteBuffer buf = ByteBuffer.wrap(data);
        int metaSize = buf.getInt(CodecHeader.HEADER_SIZE);
        int index = CodecHeader.HEADER_SIZE + 4;
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
            Serializer serializer = SerializerHolder.getSerializerById(header.getSerializerId());
            if (header.isRequest()) {
                // If method has arguments
                obj = new DeserializableArgs(serializer, body);
            } else {
                // If method has result type
                String returnType = metaMap.remove(M_RETURN_TYPE);
                Class<?> clz = MethodParameterUtils.forName(returnType);
                obj = new DeserializableResult(serializer, body, clz);
            }
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
        request.setInterfaceName(metaMap.remove(M_INTERFACE));
        request.setMethodName(metaMap.remove(M_METHOD));
        request.setMethodParameters(metaMap.remove(M_METHOD_PARAMETERS));
        request.setOptions(metaMap);
        // todo: check usage
        request.setProtocolVersion(ProtocolVersion.VERSION_2.getVersion());
        request.setSerializerId(header.getSerializerId());
        if (obj != null) {
            request.setMethodArguments(new Object[]{obj});
        }
        return request;
    }

    private Object decodeResponse(CodecHeader header, Map<String, String> metaMap, Object result) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(header.getRequestId());
        response.setElapsedTime(MathUtils.parseLong(metaMap.remove(M_ELAPSED_TIME), 0));
        response.setOptions(metaMap);
        if (CodecHeader.MessageStatus.NORMAL.getStatus() == header.getStatus()) {
            // 解析正常消息
            response.setResult(result);
        } else {
            String errorMsg = metaMap.remove(M_ERROR);
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
