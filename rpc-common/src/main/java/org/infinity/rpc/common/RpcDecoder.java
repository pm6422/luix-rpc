package org.infinity.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class RpcDecoder extends ByteToMessageDecoder {
    private Class<?> genericClass;

    public RpcDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    //解码方法,把字节数组转换为消息对象
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //消息的长度
        int size = in.readableBytes();
        if (size < 4) {//保证所有的消息都完全接受完成
            return;
        }
        byte[] bytes = new byte[size];
        //把传递的字节数组读取到bytes中
        in.readBytes(bytes);
        // 反序列化为对象(RPCRequest/RPCResponse对象)
        Object object = SerializationUtils.deserialize(bytes, genericClass);
        //输出对象
        out.add(object);
        //刷新缓存
        ctx.flush();
    }
}
