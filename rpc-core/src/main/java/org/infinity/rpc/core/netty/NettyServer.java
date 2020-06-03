package org.infinity.rpc.core.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.common.RpcDecoder;
import org.infinity.rpc.common.RpcEncoder;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;

/**
 * Netty server
 */
@Slf4j
public class NettyServer {
    private String host;
    private int    port;
    private String address;

    public NettyServer(String host, int port) {
        this.host = host;
        this.port = port;
        this.address = this.host + ":" + this.port;
    }

    /**
     * Start netty server
     */
    public void startNettyServer() {
        log.info("Starting the netty server");
        // Create the server communication object
        ServerBootstrap server = new ServerBootstrap();
        // Create async communication event group to establish TCP connection
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        // Create async communication event group to handle Channel I/O event
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // Configure the server
            server.group(bossGroup, workerGroup)
                    // Start a sync communication
                    .channel(NioServerSocketChannel.class)
                    // Initialize channel
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new RpcDecoder(RpcRequest.class))//1. Decode request object
                                    .addLast(new RpcEncoder(RpcResponse.class))//2. Encode response object
                                    .addLast(new NettyServerHandler());//3. Process request
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            // Start a async communication process
            ChannelFuture future = server.bind(host, port).sync();
            log.info("Started netty server on [{}]", address);
            // Program will be paused here until communication process finished
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("Failed to start netty with error: {}", e.getMessage());
        } finally {
            // Close the socket
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
