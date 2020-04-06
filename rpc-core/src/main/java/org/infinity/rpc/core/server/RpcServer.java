package org.infinity.rpc.core.server;

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
import org.infinity.rpc.core.server.annotation.Provider;
import org.infinity.rpc.registry.ZkRpcServerRegistry;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC服务器类，使用Spring
 */
@Slf4j
public class RpcServer implements ApplicationContextAware {
    private Map<String, Object> rpcProviderMap = new ConcurrentHashMap<>();
    // key: serviceInterfaceName, value: serviceImpl
    private ZkRpcServerRegistry zkRpcServerRegistry;
    private String              serverAddress;
    private String              serverIp;
    private int                 serverPort;
    private ApplicationContext  applicationContext;

    public RpcServer(int serverPort, ZkRpcServerRegistry rpcServerRegistry) {
        this.serverIp = "localhost";
        this.serverPort = serverPort;
        this.serverAddress = this.serverIp + ":" + this.serverPort;
        log.info("Starting RPC server on [{}]", serverAddress);
        this.zkRpcServerRegistry = rpcServerRegistry;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        Map<String, Object> map = this.applicationContext.getBeansWithAnnotation(Provider.class);
        if (!CollectionUtils.isEmpty(map)) {
            Set<Map.Entry<String, Object>> entries = map.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                final Class targetClass = getTargetClass(entry.getValue());
                rpcProviderMap.putIfAbsent(targetClass.getInterfaces()[0].getName(), entry.getValue());
            }
        }
    }

    private Class getTargetClass(Object bean) {
        if (isProxyBean(bean)) {
            return AopUtils.getTargetClass(bean);
        }
        return bean.getClass();
    }

    private boolean isProxyBean(Object bean) {
        return AopUtils.isAopProxy(bean);
    }

    /**
     * Start netty server
     */
    public void startNettyServer() {
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
                                    .addLast(new RpcServerHandler(rpcProviderMap));//3.请求处理
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ;
            // 开启异步通信服务
            ChannelFuture future = server.bind(serverIp, serverPort).sync();

            // Register RPC server on registry
            this.registerRpcServer();
            log.info("Started RPC server on [{}]", serverAddress);
            // 线程阻塞在此，程序暂停执行，等待通信完成
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("Failed to start netty", e.getMessage());
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
        log.info("Registering RPC server address [{}] on registry", serverAddress);
        zkRpcServerRegistry.createRpcServerNode(serverAddress);
        zkRpcServerRegistry.checkRegisteredRpcServer();
        log.info("Registered RPC server address [{}] on registry", serverAddress);
    }
}
