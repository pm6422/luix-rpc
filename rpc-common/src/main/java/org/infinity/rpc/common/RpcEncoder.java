package org.infinity.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcEncoder extends MessageToByteEncoder {
    private Class genericClass;

    public RpcEncoder(Class genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (genericClass.isInstance(msg)) {
            //序列化请求消息为字节数组
            byte[] bytes = SerializationUtils.serialize(msg);
            // 把数据写入到下一个通道(channel)或者是发往服务端
            out.writeBytes(bytes);
        }
    }
}
