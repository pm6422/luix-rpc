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
import org.infinity.rpc.register.RpcZookeeperRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC服务器类，使用Spring
 */
public class RpcServer implements ApplicationContextAware, InitializingBean {
    private static final Logger               LOGGER         = LoggerFactory.getLogger(RpcServer.class);
    // 用于保存所有提供服务的方法，其中key为类的全路径名，value是所有的实现类
    private final        Map<String, Object>  serviceBeanMap = new HashMap<>();
    private              RpcZookeeperRegistry rpcZookeeperRegistry;
    private              String               serverAddress;

    public RpcZookeeperRegistry getRpcZookeeperRegistry() {
        return rpcZookeeperRegistry;
    }

    public void setRpcZookeeperRegistry(RpcZookeeperRegistry rpcZookeeperRegistry) {
        this.rpcZookeeperRegistry = rpcZookeeperRegistry;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 获取到所有使用RpcService注解的Bean对象
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceImpl : serviceBeanMap.values()) {
                // 获取到类的路径名称
                String interfaceName = serviceImpl.getClass().getAnnotation(RpcService.class).value().getName();
                // 存放格式{接口名：实现类对象}
                this.serviceBeanMap.put(interfaceName, serviceImpl);
            }
        }
        LOGGER.debug("Service provider {} contains the service list {}", serverAddress, serviceBeanMap);
    }

    // 初始化完成后执行
    @Override
    public void afterPropertiesSet() throws Exception {
        // 创建服务端的通信对象
        ServerBootstrap server = new ServerBootstrap();
        // 创建异步通信的事件组 用于建立TCP连接的
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        // 创建异步通信的事件组，用于处理Channel(通道)的I/O事件
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //开始设置server的相关参数
            server.group(bossGroup, workerGroup)
                    //启动异步ServerSocket
                    .channel(NioServerSocketChannel.class)
                    //初始化通道信息
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
            String[] hostAndPortParts = serverAddress.split(":");
            ChannelFuture future = server.bind(hostAndPortParts[0], Integer.valueOf(hostAndPortParts[1])).sync();//开启异步通信服务
            LOGGER.info("RPC server {} started successfully.", future.channel().localAddress());
            LOGGER.debug("Registering service providers info on registry");
            rpcZookeeperRegistry.createNode(serverAddress);
            LOGGER.debug("Registered service providers info on registry");
            future.channel().closeFuture().sync();// 等待通信完成
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //优雅的关闭socket
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
