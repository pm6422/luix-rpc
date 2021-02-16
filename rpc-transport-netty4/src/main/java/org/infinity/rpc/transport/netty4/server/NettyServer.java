package org.infinity.rpc.transport.netty4.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.TransportException;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.exchange.transport.callback.StatisticCallback;
import org.infinity.rpc.core.exchange.transport.constants.ChannelState;
import org.infinity.rpc.core.exchange.transport.server.AbstractServer;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.transport.netty4.NettyDecoder;
import org.infinity.rpc.transport.netty4.NettyEncoder;
import org.infinity.rpc.transport.netty4.NettyServerClientHandler;
import org.infinity.rpc.utilities.threadpool.DefaultThreadFactory;
import org.infinity.rpc.utilities.threadpool.StandardThreadExecutor;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NettyServer extends AbstractServer implements StatisticCallback {
    protected NettyServerChannelManage channelManage          = null;
    private   EventLoopGroup           bossGroup;
    private   EventLoopGroup           workerGroup;
    private   Channel                  serverChannel;
    private   MessageHandler           messageHandler;
    private   StandardThreadExecutor   standardThreadExecutor = null;

    private AtomicInteger rejectCounter = new AtomicInteger(0);

    public AtomicInteger getRejectCounter() {
        return rejectCounter;
    }

    public NettyServer(Url url, MessageHandler messageHandler) {
        super(url);
        this.messageHandler = messageHandler;
    }

    @Override
    public boolean isBound() {
        return serverChannel != null && serverChannel.isActive();
    }

    @Override
    public Responseable request(Requestable request) throws TransportException {
        throw new RpcFrameworkException("NettyServer request(Request request) method not support: url: " + url);
    }

    @Override
    public boolean open() {
        if (isActive()) {
            log.warn("Netty server channel already be opened for url [{}]", url);
            return state.isActive();
        }
        if (bossGroup == null) {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
        }

        log.info("NettyServer ServerChannel start Open: url=" + url);
        boolean shareChannel = url.getBooleanOption(Url.PARAM_SHARE_CHANNEL, Url.PARAM_SHARE_CHANNEL_DEFAULT_VALUE);
        final int maxContentLength = url.getIntOption(Url.PARAM_MAX_CONTENT_LENGTH, Url.PARAM_MAX_CONTENT_LENGTH_DEFAULT_VALUE);
        int maxServerConnection = url.getIntOption(Url.PARAM_MAX_SERVER_CONNECTION, Url.PARAM_MAX_SERVER_CONNECTION_DEFAULT_VALUE);
        int workerQueueSize = url.getIntOption(Url.PARAM_WORKER_QUEUE_SIZE, Url.PARAM_WORKER_QUEUE_SIZE_DEFAULT_VALUE);

        int minWorkerThread, maxWorkerThread;

        if (shareChannel) {
            minWorkerThread = url.getIntOption(Url.PARAM_MIN_WORKER_THREAD, RpcConstants.NETTY_SHARECHANNEL_MIN_WORKDER);
            maxWorkerThread = url.getIntOption(Url.PARAM_MAX_WORKER_THREAD, RpcConstants.NETTY_SHARECHANNEL_MAX_WORKDER);
        } else {
            minWorkerThread = url.getIntOption(Url.PARAM_MIN_WORKER_THREAD, RpcConstants.NETTY_NOT_SHARECHANNEL_MIN_WORKDER);
            maxWorkerThread = url.getIntOption(Url.PARAM_MAX_WORKER_THREAD, RpcConstants.NETTY_NOT_SHARECHANNEL_MAX_WORKDER);
        }

        standardThreadExecutor = (standardThreadExecutor != null && !standardThreadExecutor.isShutdown()) ? standardThreadExecutor
                : new StandardThreadExecutor(minWorkerThread, maxWorkerThread, workerQueueSize, new DefaultThreadFactory("NettyServer-" + url.getAddress(), true));
        standardThreadExecutor.prestartAllCoreThreads();

        channelManage = new NettyServerChannelManage(maxServerConnection);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("channel_manage", channelManage);
                        pipeline.addLast("decoder", new NettyDecoder(codec, NettyServer.this, maxContentLength));
                        pipeline.addLast("encoder", new NettyEncoder());
                        NettyServerClientHandler handler = new NettyServerClientHandler(NettyServer.this, messageHandler, standardThreadExecutor);
                        pipeline.addLast("handler", handler);
                    }
                });
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(url.getPort()));
        channelFuture.syncUninterruptibly();
        serverChannel = channelFuture.channel();
        state = ChannelState.ACTIVE;
//        StatsUtils.registryStatisticCallback(this);
        log.info("NettyServer ServerChannel finish Open: url=" + url);
        return state.isActive();
    }

    @Override
    public synchronized void close() {
        close(0);
    }

    @Override
    public synchronized void close(int timeout) {
        if (state.isClosed()) {
            return;
        }

        try {
            cleanup();
            if (state.isUninitialized()) {
                log.info("NettyServer close fail: state={}, url={}", state.value, url.getUri());
                return;
            }

            // 设置close状态
            state = ChannelState.CLOSED;
            log.info("NettyServer close Success: url={}", url.getUri());
        } catch (Exception e) {
            log.error("NettyServer close Error: url=" + url.getUri(), e);
        }
    }

    @Override
    public ChannelState getState() {
        return state;
    }

    public void cleanup() {
        // close listen socket
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        // close all clients's channel
        if (channelManage != null) {
            channelManage.close();
        }
        // shutdown the threadPool
        if (standardThreadExecutor != null) {
            standardThreadExecutor.shutdownNow();
        }
        // 取消统计回调的注册
//        StatsUtil.unRegistryStatisticCallback(this);
    }

    @Override
    public boolean isClosed() {
        return state.isClosed();
    }

    @Override
    public boolean isActive() {
        return state.isActive();
    }

    @Override
    public Url getProviderUrl() {
        return url;
    }

    @Override
    public String statisticCallback() {
        return String.format("identity: %s connectionCount: %s taskCount: %s queueCount: %s maxThreadCount: %s maxTaskCount: %s executorRejectCount: %s",
                url.getIdentity(), channelManage.getChannels().size(), standardThreadExecutor.getSubmittedTasksCount(),
                standardThreadExecutor.getQueue().size(), standardThreadExecutor.getMaximumPoolSize(),
                standardThreadExecutor.getMaxSubmittedTasksCount(), rejectCounter.getAndSet(0));
    }
}
