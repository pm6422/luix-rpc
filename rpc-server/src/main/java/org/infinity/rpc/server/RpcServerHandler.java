package org.infinity.rpc.server;


import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public class RpcServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger              LOGGER = LoggerFactory.getLogger(RpcServerHandler.class);
    private              Map<String, Object> serviceBeanMap;

    public RpcServerHandler(Map<String, Object> serviceBeanMap) {
        this.serviceBeanMap = serviceBeanMap;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        LOGGER.debug("RpcServerHandler.channelRead: {}", msg);
        RpcRequest rpcRequest = (RpcRequest) msg;
        RpcResponse rpcResponse = this.processRequest(rpcRequest);
        //告诉客户端，关闭socket连接
        ctx.writeAndFlush(rpcResponse).addListener(ChannelFutureListener.CLOSE);
    }

    private RpcResponse processRequest(RpcRequest rpcRequest) {
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setResponseId(UUID.randomUUID().toString());
        rpcResponse.setRequestId(rpcRequest.getRequestId());
        try {
            String className = rpcRequest.getClassName();
            String methodName = rpcRequest.getMethodName();
            Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
            Object[] args = rpcRequest.getArgs();
            // 获取到具字节码对象
            Class<?> clz = Class.forName(className);
            // 获取到实现类
            Object serviceImpl = serviceBeanMap.get(className);
            if (serviceImpl == null) {
                throw new RuntimeException("Service bean can NOT be found with name: ".concat(className));
            }
            Method method = clz.getMethod(methodName, parameterTypes);
            if (method == null) {
                throw new RuntimeException("Method can NOT be found with name: ".concat(methodName));
            }
            // JDK反射调用
            Object result = method.invoke(serviceImpl, args);
            rpcResponse.setSuccess(true);
            // 设置方法调用结果
            rpcResponse.setResult(result);
        } catch (Exception e) {
            rpcResponse.setSuccess(false);
            rpcResponse.setThrowable(e);
            e.printStackTrace();
        }
        return rpcResponse;
    }
}
