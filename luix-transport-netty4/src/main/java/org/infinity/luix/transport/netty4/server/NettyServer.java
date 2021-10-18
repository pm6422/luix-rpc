package org.infinity.luix.transport.netty4.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.client.request.Requestable;
import org.infinity.luix.core.exception.TransportException;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.callback.StatisticCallback;
import org.infinity.luix.core.exchange.constants.ChannelState;
import org.infinity.luix.core.exchange.server.AbstractServer;
import org.infinity.luix.core.server.handler.InvocationHandleable;
import org.infinity.luix.core.server.response.Responseable;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.transport.netty4.NettyDecoder;
import org.infinity.luix.transport.netty4.NettyEncoder;
import org.infinity.luix.transport.netty4.NettyServerClientHandler;
import org.infinity.luix.utilities.threadpool.DefaultThreadFactory;
import org.infinity.luix.utilities.threadpool.StandardThreadExecutor;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.infinity.luix.core.constant.ProtocolConstants.*;

@Slf4j
public class NettyServer extends AbstractServer implements StatisticCallback {
    protected NettyServerChannelManage   channelManage;
    private   EventLoopGroup             bossGroup;
    private   EventLoopGroup             workerGroup;
    private Channel                serverChannel;
    private InvocationHandleable   handler;
    private StandardThreadExecutor standardThreadExecutor;

    private AtomicInteger rejectCounter = new AtomicInteger(0);

    public AtomicInteger getRejectCounter() {
        return rejectCounter;
    }

    public NettyServer(Url providerUrl, InvocationHandleable handler) {
        super(providerUrl);
        this.handler = handler;
    }

    @Override
    public boolean isBound() {
        return serverChannel != null && serverChannel.isActive();
    }

    @Override
    public Responseable request(Requestable request) throws TransportException {
        throw new RpcFrameworkException("NettyServer request(Request request) method not support: url: " + providerUrl);
    }

    @Override
    public boolean open() {
        if (isActive()) {
            log.warn("Netty server channel already be opened for url [{}]", providerUrl);
            return state.isActive();
        }
        if (bossGroup == null) {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
        }

        log.info("NettyServer ServerChannel start Open: url=" + providerUrl);
        int maxContentLength = providerUrl.getIntOption(MAX_CONTENT_LENGTH, MAX_CONTENT_LENGTH_VAL_DEFAULT);
        int maxServerConn = providerUrl.getIntOption(MAX_SERVER_CONN, MAX_SERVER_CONN_VAL_DEFAULT);
        int workerQueueSize = providerUrl.getIntOption(WORK_QUEUE_SIZE, WORK_QUEUE_SIZE_VAL_DEFAULT);
        boolean shareChannel = providerUrl.getBooleanOption(SHARED_SERVER, SHARED_SERVER_VAL_DEFAULT);

        int minWorkerThread, maxWorkerThread;

        if (shareChannel) {
            minWorkerThread = providerUrl.getIntOption(MIN_THREAD, MIN_THREAD_SHARED_CHANNEL);
            maxWorkerThread = providerUrl.getIntOption(MAX_THREAD, MAX_THREAD_SHARED_CHANNEL);
        } else {
            minWorkerThread = providerUrl.getIntOption(MIN_THREAD, MIN_THREAD_VAL_DEFAULT);
            maxWorkerThread = providerUrl.getIntOption(MAX_THREAD, MAX_THREAD_VAL_DEFAULT);
        }

        standardThreadExecutor = (standardThreadExecutor != null && !standardThreadExecutor.isShutdown()) ? standardThreadExecutor
                : new StandardThreadExecutor(minWorkerThread, maxWorkerThread, workerQueueSize, new DefaultThreadFactory("NettyServer-" + providerUrl.getAddress(), true));
        standardThreadExecutor.prestartAllCoreThreads();

        channelManage = new NettyServerChannelManage(maxServerConn);

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
                        NettyServerClientHandler handler = new NettyServerClientHandler(NettyServer.this, NettyServer.this.handler, standardThreadExecutor);
                        pipeline.addLast("handler", handler);
                    }
                });
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(providerUrl.getPort()));
        channelFuture.syncUninterruptibly();
        serverChannel = channelFuture.channel();
        state = ChannelState.ACTIVE;
//        StatsUtils.registryStatisticCallback(this);
        log.info("NettyServer ServerChannel finish Open: url=" + providerUrl);
        log.info("Started netty server with port [{}]", providerUrl.getPort());
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
            if (state.isCreated()) {
                log.info("NettyServer close fail: state={}, url={}", state.value, providerUrl.getUri());
                return;
            }

            // 设置close状态
            state = ChannelState.CLOSED;
            log.info("NettyServer close Success: url={}", providerUrl.getUri());
        } catch (Exception e) {
            log.error("NettyServer close Error: url=" + providerUrl.getUri(), e);
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
        return providerUrl;
    }

    @Override
    public String statisticCallback() {
        return String.format("identity: %s connectionCount: %s taskCount: %s queueCount: %s maxThreadCount: %s maxTaskCount: %s executorRejectCount: %s",
                providerUrl.getIdentity(), channelManage.getChannels().size(), standardThreadExecutor.getSubmittedTasksCount(),
                standardThreadExecutor.getQueue().size(), standardThreadExecutor.getMaximumPoolSize(),
                standardThreadExecutor.getMaxSubmittedTasksCount(), rejectCounter.getAndSet(0));
    }
}
