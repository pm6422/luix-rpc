package org.infinity.rpc.core.server;


import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
public class RpcServerHandler extends ChannelInboundHandlerAdapter {

    public RpcServerHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.debug("Received RPC request: {}", msg);
        RpcRequest rpcRequest = (RpcRequest) msg;
        RpcResponse rpcResponse = this.processRequest(rpcRequest);
        // 告诉客户端，关闭socket连接
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
            // 获取到实现类
            Object serviceImpl = ProviderWrapperHolder.getInstance().getWrappers().get(className).getProviderInstance();
            if (serviceImpl == null) {
                throw new RuntimeException("Service provider class can NOT be found with name: ".concat(className));
            }
            // 获取到具字节码对象
            Method method = serviceImpl.getClass().getMethod(methodName, parameterTypes);
            if (method == null) {
                throw new RuntimeException("Service provider method can NOT be found with name: ".concat(methodName));
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
