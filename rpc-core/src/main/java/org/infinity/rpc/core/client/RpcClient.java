package org.infinity.rpc.core.client;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.infinity.rpc.common.RpcDecoder;
import org.infinity.rpc.common.RpcEncoder;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.registry.ZkRpcServerRegistry;

/**
 * RPC通信客户端，向服务端发送请求，并接受服务端的响应
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {
    // 消息请求对象
    private RpcRequest          rpcRequest;
    // 消息响应对象
    private RpcResponse         rpcResponse;
    // 获取服务地址列表
    private ZkRpcServerRegistry zkRpcServerRegistry;
    // 同步锁
    private Object              object = new Object();

    //构造函数
    public RpcClient(RpcRequest rpcRequest, ZkRpcServerRegistry zkRpcServerRegistry) {
        this.rpcRequest = rpcRequest;
        this.zkRpcServerRegistry = zkRpcServerRegistry;
        this.zkRpcServerRegistry.startWatchNode();
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
            // 客户端负载均衡算法获取一个服务器地址
            String serverAddress = zkRpcServerRegistry.discoverRpcServer();
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
