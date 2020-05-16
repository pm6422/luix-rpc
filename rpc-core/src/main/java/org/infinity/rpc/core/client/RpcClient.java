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
 * RPC通信客户端，向服务端发送请求，并接受服务端的响应
 */
@Slf4j
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {
    // 消息请求对象
    private RpcRequest     rpcRequest;
    // 消息响应对象
    private RpcResponse    rpcResponse;
    // 同步锁
    private Object         object = new Object();
    private List<Registry> registries;

    //构造函数
    public RpcClient(RpcRequest rpcRequest, List<Registry> registries) {
        this.rpcRequest = rpcRequest;
        this.registries = registries;
    }

    /**
     * 发送消息
     *
     * @return 响应结果
     * @throws Exception
     */
    public RpcResponse send() throws Exception {
        // 创建一个socket通信对象
        Bootstrap client = new Bootstrap();
        // 创建一个通信组，负责Channel(通道)的I/O事件的处理
        NioEventLoopGroup loopGroup = new NioEventLoopGroup();
        try {
            client.group(loopGroup)// 设置参数
                    .channel(NioSocketChannel.class)// 使用异步socket通信
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new RpcEncoder(RpcRequest.class))//1.编码请求对象
                                    .addLast(new RpcDecoder(RpcResponse.class))//2.解码响应对象
                                    .addLast(RpcClient.this);//3.发送请求对象
                        }
                    }).option(ChannelOption.SO_KEEPALIVE, true);

            if (CollectionUtils.isEmpty(registries)) {
                return rpcResponse;
            }
            // todo: 支持多个注册中心,客户端负载均衡算法获取一个服务器地址
            Registry registry = registries.get(0);
            List<String> activeProviderAddress = registry.discoverActiveProviderAddress(rpcRequest.getClassName());
            String serverAddress = discoverRpcServer(activeProviderAddress);
            String[] hostAndPortParts = serverAddress.split(":");
            ChannelFuture future = client.connect(hostAndPortParts[0], Integer.valueOf(hostAndPortParts[1])).sync();
            future.channel().writeAndFlush(this.rpcRequest).sync();
            synchronized (object) {
                object.wait();// 线程阻塞，程序暂停继续执行，等待notify后继续执行
            }
            if (this.rpcResponse != null) {
                future.channel().closeFuture().sync();// 等待服务端关闭socket
            }
            return this.rpcResponse;
        } finally {
            loopGroup.shutdownGracefully();// 优雅关闭socket
        }
    }

    /**
     * 随机返回一台服务器地址信息，用于负载均衡
     *
     * @return
     */
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
        synchronized (object) {
            // 刷新缓存
            ctx.flush();
            // 唤醒等待
            object.notifyAll();
        }
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
