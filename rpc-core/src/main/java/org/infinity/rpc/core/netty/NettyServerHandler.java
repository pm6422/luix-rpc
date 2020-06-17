package org.infinity.rpc.core.netty;


import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.core.server.ProviderWrapperHolder;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext context, Object msg) {
        log.debug("Received RPC request: {}", msg);
        RpcRequest rpcRequest = (RpcRequest) msg;
        RpcResponse rpcResponse = this.processRequest(rpcRequest);
        // Tell the client that socket was closed
        context.writeAndFlush(rpcResponse).addListener(ChannelFutureListener.CLOSE);
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
            // Get the service instance
            Object serviceImpl = ProviderWrapperHolder.getInstance().getWrappers().get(className).getInstance();
            if (serviceImpl == null) {
                throw new RuntimeException("Service provider class can NOT be found with the name: ".concat(className));
            }
            // Get the method object via Java reflect
            Method method = serviceImpl.getClass().getMethod(methodName, parameterTypes);
            if (method == null) {
                throw new RuntimeException("Service provider method can NOT be found with the name: ".concat(methodName));
            }
            // Reflect invocation
            Object result = method.invoke(serviceImpl, args);
            rpcResponse.setSuccess(true);
            // Set the method invoke result
            rpcResponse.setResult(result);
        } catch (Exception e) {
            rpcResponse.setSuccess(false);
            rpcResponse.setThrowable(e);
            e.printStackTrace();
        }
        return rpcResponse;
    }
}
