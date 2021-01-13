package org.infinity.rpc.core.client;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.common.RpcDecoder;
import org.infinity.rpc.common.RpcEncoder;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.core.registry.Registry;

import java.util.List;
import java.util.Random;

/**
 * RPC client used to send the request from client to server, and receive the response
 */
@Slf4j
@Deprecated
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {
    /**
     * RPC request
     */
    private RpcRequest     rpcRequest;
    /**
     * RPC response
     */
    private RpcResponse    rpcResponse;
    /**
     * Thread lock
     */
    private Object         lock = new Object();
    /**
     * Registries
     */
    private List<Registry> registries;

    public RpcClient(RpcRequest rpcRequest, List<Registry> registries) {
        this.rpcRequest = rpcRequest;
        this.registries = registries;
    }

    public RpcResponse send() throws Exception {
        // Create a socket
        Bootstrap client = new Bootstrap();
        // Create async communication event group to handle Channel I/O event
        NioEventLoopGroup loopGroup = new NioEventLoopGroup();
        try {
            client.group(loopGroup)// Configure
                    .channel(NioSocketChannel.class)// Use async socket
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new RpcEncoder(RpcRequest.class))//1. Encode request object
                                    .addLast(new RpcDecoder(RpcResponse.class))//2. Decode response object
                                    .addLast(RpcClient.this);//3. Send request
                        }
                    }).option(ChannelOption.SO_KEEPALIVE, true);

            if (CollectionUtils.isEmpty(registries)) {
                return rpcResponse;
            }
            // TODO: need to support multiple registry
            Registry registry = registries.get(0);
            List<String> activeProviderAddress = registry.discoverActiveProviderAddress(rpcRequest.getClassName());
            String serverAddress = discoverRpcServer(activeProviderAddress);
            String[] hostAndPortParts = serverAddress.split(":");
            ChannelFuture future = client.connect(hostAndPortParts[0], Integer.valueOf(hostAndPortParts[1])).sync();
            future.channel().writeAndFlush(this.rpcRequest).sync();
            synchronized (lock) {
                lock.wait();// Program pause here waiting for notification event
            }
            if (this.rpcResponse != null) {
                future.channel().closeFuture().sync();// Wait for server side close the socket
            }
            return this.rpcResponse;
        } finally {
            // Close the socket
            loopGroup.shutdownGracefully();
        }
    }

    private String discoverRpcServer(List<String> providerAddresses) {
        int size = providerAddresses.size();
        if (size == 0) {
            throw new RuntimeException("No RPC server found");
        }
        int index = new Random().nextInt(size);
        String server = providerAddresses.get(index);
        log.info("Got RPC server [{}] by load balance algorithm", server);
        return server;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        this.rpcResponse = msg;
        synchronized (lock) {
            // Refresh cache
            ctx.flush();
            // Wait for notification
            lock.notifyAll();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
