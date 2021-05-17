package org.infinity.rpc.core.codec.impl;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.request.impl.RpcRequest;
import org.infinity.rpc.core.codec.AbstractCodec;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.Channel;
import org.infinity.rpc.core.exchange.Exchangable;
import org.infinity.rpc.core.protocol.constants.ProtocolVersion;
import org.infinity.rpc.utilities.serializer.Serializer;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.core.utils.MethodParameterUtils;
import org.infinity.rpc.utilities.lang.ByteUtils;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.infinity.rpc.core.constant.ProtocolConstants.*;
import static org.infinity.rpc.core.constant.ServiceConstants.MAX_PAYLOAD;
import static org.infinity.rpc.core.constant.ServiceConstants.MAX_PAYLOAD_VAL_DEFAULT;
import static org.infinity.rpc.core.exception.RpcErrorMsgConstant.ENCODE_ERROR;

@SpiName(CODEC_VAL_DEFAULT)
public class DefaultCodec extends AbstractCodec {
    public static final  short MAGIC = (short) 0xF0F0;
    private static final byte  MASK  = 0x07;

    @Override
    public byte[] encode(Channel channel, Exchangable inputObject) {
        try {
            if (inputObject instanceof RpcRequest) {
                return encodeRequest(channel, (RpcRequest) inputObject);
            } else {
                return encodeResponse(channel, (RpcResponse) inputObject);
            }
        } catch (Exception e) {
            if (ExceptionUtils.isRpcException(e)) {
                throw (RuntimeException) e;
            } else {
                throw new RpcFrameworkException("Failed to encode input object: " + inputObject, e, ENCODE_ERROR);
            }
        }
    }

    private byte[] encodeRequest(Channel channel, RpcRequest request) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput output = createOutputStream(outputStream);

        output.writeUTF(request.getInterfaceName());
        output.writeUTF(request.getMethodName());
        output.writeUTF(request.getMethodParameters());

        if (ArrayUtils.isNotEmpty(request.getMethodArguments())) {
            for (Object arg : request.getMethodArguments()) {
                // Serialize method arguments
                serialize(getSerializer(channel), output, arg);
            }
        }

        if (MapUtils.isEmpty(request.getOptions())) {
            // No options
            output.writeInt(0);
        } else {
            // Write options
            output.writeInt(request.getOptions().size());
            for (Map.Entry<String, String> entry : request.getOptions().entrySet()) {
                output.writeUTF(entry.getKey());
                output.writeUTF(entry.getValue());
            }
        }

        output.flush();
        byte[] body = outputStream.toByteArray();
        output.close();

        // Check max request payload size
        checkMessagePayloadSize(body.length, request.getIntOption(MAX_PAYLOAD, MAX_PAYLOAD_VAL_DEFAULT));
        return encode(body, RpcConstants.FLAG_REQUEST, request.getRequestId());
    }

    /**
     * 数据协议：
     *
     * <pre>
     *
     * header:  16个字节
     *
     * 0-15 bit 	:  magic
     * 16-23 bit	:  version
     * 24-31 bit	:  extend flag , 其中： 29-30 bit: event 可支持4种event，比如normal, exception等,  31 bit : 0 is request , 1 is response
     * 32-95 bit 	:  request id
     * 96-127 bit 	:  body content length
     *
     * </pre>
     *
     * @param body      message body
     * @param flag      flag
     * @param requestId request ID
     * @return encoded bytes
     */
    private byte[] encode(byte[] body, byte flag, long requestId) {
        byte[] header = new byte[ProtocolVersion.VERSION_1.getHeaderLength()];
        int offset = 0;

        // 0 - 15 bit : magic
        ByteUtils.short2bytes(MAGIC, header, offset);
        offset += 2;

        // 16 - 23 bit : version
        header[offset++] = ProtocolVersion.VERSION_1.getVersion();

        // 24 - 31 bit : extend flag
        header[offset++] = flag;

        // 32 - 95 bit : requestId
        ByteUtils.long2bytes(requestId, header, offset);
        offset += 8;

        // 96 - 127 bit : body content length
        ByteUtils.int2bytes(body.length, header, offset);
        byte[] data = new byte[header.length + body.length];

        System.arraycopy(header, 0, data, 0, header.length);
        System.arraycopy(body, 0, data, header.length, body.length);
        return data;
    }

    @Override
    public Object decode(Channel channel, String remoteIp, byte[] data) {
        if (data.length <= ProtocolVersion.VERSION_1.getHeaderLength()) {
            throw new RpcFrameworkException("decode error: format problem", RpcErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }

        short type = ByteUtils.bytes2short(data, 0);

        if (type != MAGIC) {
            throw new RpcFrameworkException("decode error: magic error", RpcErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }

        if (data[2] != ProtocolVersion.VERSION_1.getVersion()) {
            throw new RpcFrameworkException("decode error: version error", RpcErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }

        int bodyLength = ByteUtils.bytes2int(data, 12);

        if (ProtocolVersion.VERSION_1.getHeaderLength() + bodyLength != data.length) {
            throw new RpcFrameworkException("decode error: content length error", RpcErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }

        byte flag = data[3];
        byte dataType = (byte) (flag & MASK);
        boolean isResponse = (dataType != RpcConstants.FLAG_REQUEST);

        byte[] body = new byte[bodyLength];
        System.arraycopy(data, ProtocolVersion.VERSION_1.getHeaderLength(), body, 0, bodyLength);

        long requestId = ByteUtils.bytes2long(data, 4);
        Serializer serializer = getSerializer(channel);

        try {
            if (isResponse) {
                return decodeResponse(body, dataType, requestId, serializer);
            } else {
                return decodeRequest(body, requestId, serializer);
            }
        } catch (ClassNotFoundException e) {
            throw new RpcFrameworkException("decode " + (isResponse ? "response" : "request") + " error: class not found", e,
                    RpcErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        } catch (Exception e) {
            if (ExceptionUtils.isRpcException(e)) {
                throw (RuntimeException) e;
            } else {
                throw new RpcFrameworkException("decode error: isResponse=" + isResponse, e, RpcErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
            }
        }
    }

    private Serializer getSerializer(Channel channel) {
        String serializerName = channel.getProviderUrl().getOption(SERIALIZER, SERIALIZER_VAL_DEFAULT);
        return Serializer.getInstance(serializerName);
    }

    private byte[] encodeResponse(Channel channel, RpcResponse value) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput output = createOutputStream(outputStream);
        Serializer serializer = getSerializer(channel);

        byte flag;
        output.writeLong(value.getElapsedTime());

        if (value.getException() != null) {
            output.writeUTF(value.getException().getClass().getName());
            serialize(serializer, output, value.getException());
            flag = RpcConstants.FLAG_RESPONSE_EXCEPTION;
        } else if (value.getResult() == null) {
            flag = RpcConstants.FLAG_RESPONSE_VOID;
        } else {
            output.writeUTF(value.getResult().getClass().getName());
            serialize(serializer, output, value.getResult());
            flag = RpcConstants.FLAG_RESPONSE;
        }

        output.flush();
        byte[] body = outputStream.toByteArray();
        output.close();

        // Check max response payload size
        checkMessagePayloadSize(body.length, channel.getProviderUrl().getIntOption(MAX_PAYLOAD, MAX_PAYLOAD_VAL_DEFAULT));
        return encode(body, flag, value.getRequestId());
    }

    private Object decodeRequest(byte[] body, long requestId, Serializer serializer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        ObjectInput input = createInput(inputStream);

        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId(requestId);
        rpcRequest.setInterfaceName(input.readUTF());
        rpcRequest.setMethodName(input.readUTF());
        rpcRequest.setMethodParameters(input.readUTF());
        rpcRequest.setMethodArguments(decodeMethodArgs(input, rpcRequest.getMethodParameters(), serializer));
        rpcRequest.setOptions(decodeRequestAttachments(input));

        input.close();
        return rpcRequest;
    }

    private Object[] decodeMethodArgs(ObjectInput objectInput, String parameterTypeList, Serializer serializer)
            throws IOException, ClassNotFoundException {
        if (StringUtils.isEmpty(parameterTypeList)) {
            return null;
        }

        Class<?>[] classTypes = MethodParameterUtils.forNames(parameterTypeList);
        Object[] paramObjs = new Object[classTypes.length];
        for (int i = 0; i < classTypes.length; i++) {
            paramObjs[i] = deserialize((byte[]) objectInput.readObject(), classTypes[i], serializer);
        }
        return paramObjs;
    }

    private Map<String, String> decodeRequestAttachments(ObjectInput input) throws IOException {
        int size = input.readInt();
        if (size <= 0) {
            return new ConcurrentHashMap<>(10);
        }

        Map<String, String> attachments = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            attachments.put(input.readUTF(), input.readUTF());
        }
        return attachments;
    }

    private Object decodeResponse(byte[] body, byte dataType, long requestId, Serializer serializer) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        ObjectInput input = createInput(inputStream);

        long processTime = input.readLong();
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setElapsedTime(processTime);

        if (dataType == RpcConstants.FLAG_RESPONSE_VOID) {
            return response;
        }

        String className = input.readUTF();
        Class<?> clz = MethodParameterUtils.forName(className);
        Object result = deserialize((byte[]) input.readObject(), clz, serializer);
        if (dataType == RpcConstants.FLAG_RESPONSE) {
            response.setResultObject(result);
        } else if (dataType == RpcConstants.FLAG_RESPONSE_EXCEPTION) {
            response.setException((Exception) result);
        } else {
            throw new RpcFrameworkException("decode error: response dataType not support " + dataType,
                    RpcErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
        }

        response.setRequestId(requestId);
        input.close();
        return response;
    }
}
