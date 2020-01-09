package org.infinity.rpc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.collections4.MapUtils;
import org.infinity.rpc.common.RpcDecoder;
import org.infinity.rpc.common.RpcEncoder;
import org.infinity.rpc.common.RpcRequest;
import org.infinity.rpc.common.RpcResponse;
import org.infinity.rpc.registry.ZkRpcServerRegistry;
import org.infinity.rpc.server.annotation.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC服务器类，使用Spring
 */
public class RpcServer implements ApplicationContextAware, InitializingBean {
    private static final Logger              LOGGER         = LoggerFactory.getLogger(RpcServer.class);
    // key: serviceInterfaceName, value: serviceImpl
    private final        Map<String, Object> serviceBeanMap = new HashMap<>();
    private              ZkRpcServerRegistry zkRpcServerRegistry;
    private              String              serverAddress;
    private              String              serverIp;
    private              int                 serverPort;

    public RpcServer(String serverAddress, ZkRpcServerRegistry rpcServerRegistry) {
        LOGGER.info("Starting RPC server on [{}]", serverAddress);
        this.serverAddress = serverAddress;
        String[] ipAndPortParts = serverAddress.split(":");
        serverIp = ipAndPortParts[0];
        serverPort = Integer.valueOf(ipAndPortParts[1]);
        this.zkRpcServerRegistry = rpcServerRegistry;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.discoverRpcService(applicationContext);
    }

    private void discoverRpcService(ApplicationContext applicationContext) {
        // get all beans with the annotation
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(Provider.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceImpl : serviceBeanMap.values()) {
                final Class<?>[] interfaces = serviceImpl.getClass().getInterfaces();
                String serviceInterfaceName;
                if (interfaces.length == 1) {
                    serviceInterfaceName = interfaces[0].getName();
                } else {
                    // Get service interface from annotation if a instance has more than one declared interfaces
                    serviceInterfaceName = serviceImpl.getClass().getAnnotation(Provider.class).interfaceClass().getName();
                }
                this.serviceBeanMap.put(serviceInterfaceName, serviceImpl);
                LOGGER.info("Discovering RPC Service provider [{}]", serviceImpl.getClass().getName());
            }
        }
        LOGGER.info("Discovered all RPC service providers");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.startNettyServer();
    }

    /**
     * Start netty server
     */
    private void startNettyServer() {
        // 创建服务端的通信对象
        ServerBootstrap server = new ServerBootstrap();
        // 创建异步通信的事件组 用于建立TCP连接的
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        // 创建异步通信的事件组，用于处理Channel(通道)的I/O事件
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 开始设置server的相关参数
            server.group(bossGroup, workerGroup)
                    // 启动异步ServerSocket
                    .channel(NioServerSocketChannel.class)
                    // 初始化通道信息
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new RpcDecoder(RpcRequest.class))//1.解码请求对象
                                    .addLast(new RpcEncoder(RpcResponse.class))//2.编码响应对象
                                    .addLast(new RpcServerHandler(serviceBeanMap));//3.请求处理
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ;
            // 开启异步通信服务
            ChannelFuture future = server.bind(serverIp, serverPort).sync();

            // Register RPC server on registry
            this.registerRpcServer();
            LOGGER.info("Started RPC server on [{}]", serverAddress);
            // 线程阻塞在此，程序暂停执行，等待通信完成
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error("Failed to start netty", e.getMessage());
        } finally {
            // 优雅的关闭socket
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * Register RPC server on registry
     *
     * @throws Exception
     */
    private void registerRpcServer() throws Exception {
        LOGGER.info("Registering RPC server address [{}] on registry", serverAddress);
        zkRpcServerRegistry.createRpcServerNode(serverAddress);
        LOGGER.info("Registered RPC server address [{}] on registry", serverAddress);
    }
}
