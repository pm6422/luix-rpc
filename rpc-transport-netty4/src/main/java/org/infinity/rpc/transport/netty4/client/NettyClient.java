package org.infinity.rpc.transport.netty4.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.destroy.ScheduledThreadPool;
import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.Channel;
import org.infinity.rpc.core.exchange.client.AbstractSharedPoolClient;
import org.infinity.rpc.core.exchange.client.SharedObjectFactory;
import org.infinity.rpc.core.exchange.constants.ChannelState;
import org.infinity.rpc.core.server.response.FutureResponse;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;
import org.infinity.rpc.transport.netty4.NettyDecoder;
import org.infinity.rpc.transport.netty4.NettyEncoder;
import org.infinity.rpc.transport.netty4.NettyServerClientHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.infinity.rpc.core.constant.ProtocolConstants.*;
import static org.infinity.rpc.core.constant.RegistryConstants.CONNECT_TIMEOUT;
import static org.infinity.rpc.core.constant.RegistryConstants.CONNECT_TIMEOUT_VAL_DEFAULT;
import static org.infinity.rpc.core.destroy.ScheduledThreadPool.RECYCLE_TIMEOUT_TASK_THREAD_POOL;

/**
 * toto: implements StatisticCallback
 */
@Slf4j
public class NettyClient extends AbstractSharedPoolClient {
    private static final int                       NETTY_TIMEOUT_TIMER_INTERVAL = 100;
    private static final NioEventLoopGroup         NIO_EVENT_LOOP_GROUP         = new NioEventLoopGroup();
    /**
     * 异步的request，需要注册callback future
     * 触发remove的操作有： 1) service的返回结果处理。 2) timeout thread cancel
     */
    protected            Map<Long, FutureResponse> callbackMap                  = new ConcurrentHashMap<>();
    /**
     * 连续失败次数
     */
    private final        AtomicLong                errorCount                   = new AtomicLong(0);
    private final        ScheduledFuture<?>        timeoutFuture;
    private              Bootstrap                 bootstrap;
    private final        int                       maxClientFailedConn;

    public NettyClient(Url providerUrl) {
        super(providerUrl);
        maxClientFailedConn = providerUrl.getIntOption(MAX_CLIENT_FAILED_CONN, MAX_CLIENT_FAILED_CONN_VAL_DEFAULT);
        timeoutFuture = ScheduledThreadPool.schedulePeriodicalTask(RECYCLE_TIMEOUT_TASK_THREAD_POOL,
                NETTY_TIMEOUT_TIMER_INTERVAL, this::recycleTimeoutTask);
    }

    private void recycleTimeoutTask() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<Long, FutureResponse> entry : callbackMap.entrySet()) {
            try {
                FutureResponse future = entry.getValue();
                if (future.getCreatedTime() + future.getTimeout() < currentTime) {
                    // timeout: remove from callback list, and then cancel
                    removeCallback(entry.getKey());
                    future.cancel();
                }
            } catch (Exception e) {
                log.error("Clear timeout future Error: uri=" + providerUrl.getUri() + " requestId=" + entry.getKey(), e);
            }
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected SharedObjectFactory createChannelFactory() {
        return new NettyChannelFactory(this);
    }

    @Override
    public Responseable request(Requestable request) {
        if (!isActive()) {
            throw new RpcServiceException("NettyChannel is unavailable: url=" + providerUrl.getUri() + request);
        }
        return doRequest(request);
    }

    private Responseable doRequest(Requestable request) {
        Channel channel;
        Responseable response;
        try {
            // return channel or throw exception(timeout or connection_fail)
            channel = getChannel();
            RpcFrameworkUtils.logEvent(request, RpcConstants.TRACE_CONNECTION);

            if (channel == null) {
                log.error("NettyClient borrowObject null: url=" + providerUrl.getUri() + " " + request);
                return null;
            }

            // All requests are handled asynchronously, and return type always be RpcFutureResponse
            response = channel.request(request);
        } catch (Exception e) {
            log.error("NettyClient request Error: url=" + providerUrl.getUri() + " " + request + ", " + e.getMessage());

            if (e instanceof RpcAbstractException) {
                throw (RpcAbstractException) e;
            } else {
                throw new RpcServiceException("NettyClient request Error: url=" + providerUrl.getUri() + " " + request, e);
            }
        }

        // Return RpcFutureResponse directly or convert RpcFutureResponse to RpcResponse
        response = asyncResponse(response, request.isAsync());
        return response;
    }

    /**
     *
     *
     * @param response response
     * @param async    async flag
     * @return response
     */
    private Responseable asyncResponse(Responseable response, boolean async) {
        if (async) {
            // If it is asynchronous call, return RpcFutureResponse directly.
            return response;
        }
        // If it is synchronous call, firstly it takes time to convert RpcFutureResponse to RpcResponse, and then return it.
        return RpcResponse.of(response);
    }

    @Override
    public synchronized boolean open() {
        if (isActive()) {
            return true;
        }

        bootstrap = new Bootstrap();
        int timeout = getProviderUrl().getIntOption(CONNECT_TIMEOUT, CONNECT_TIMEOUT_VAL_DEFAULT);
        if (timeout <= 0) {
            throw new RpcFrameworkException("NettyClient init Error: timeout(" + timeout + ") <= 0 is forbid.", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        // 最大响应包限制
        final int maxContentLength = providerUrl.getIntOption(MAX_CONTENT_LENGTH, MAX_CONTENT_LENGTH_VAL_DEFAULT);
        bootstrap.group(NIO_EVENT_LOOP_GROUP)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new NettyDecoder(codec, NettyClient.this, maxContentLength));
                        pipeline.addLast("encoder", new NettyEncoder());
                        pipeline.addLast("handler", new NettyServerClientHandler(NettyClient.this, (channel, message) -> {
                            Responseable response = (Responseable) message;
                            FutureResponse futureResponse = NettyClient.this.removeCallback(response.getRequestId());
                            if (futureResponse == null) {
                                log.warn("NettyClient has response from server, but responseFuture not exist, requestId={}", response.getRequestId());
                                return null;
                            }
                            if (response.getException() != null) {
                                futureResponse.onFailure(response);
                            } else {
                                futureResponse.onSuccess(response);
                            }
                            return null;
                        }));
                    }
                });

        // 初始化连接池
        initPool();

        log.info("NettyClient finish Open: url={}", providerUrl);

//         注册统计回调
//        StatsUtil.registryStatisticCallback(this);
//         设置可用状态

        state = ChannelState.ACTIVE;
        return true;
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
                log.info("NettyClient close fail: state={}, url={}", state.value, providerUrl.getUri());
                return;
            }

            // 设置close状态
            state = ChannelState.CLOSED;
            log.info("NettyClient close Success: url={}", providerUrl.getUri());
        } catch (Exception e) {
            log.error("NettyClient close Error: url=" + providerUrl.getUri(), e);
        }
    }

    public void cleanup() {
        // 取消定期的回收任务
        timeoutFuture.cancel(true);
        // 清空callback
        callbackMap.clear();
        // 关闭client持有的channel
        closeAllChannels();
        // 解除统计回调的注册
//        StatsUtil.unRegistryStatisticCallback(this);
    }

    @Override
    public ChannelState getState() {
        return state;
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

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public FutureResponse removeCallback(long requestId) {
        return callbackMap.remove(requestId);
    }

    /**
     * 增加调用失败的次数：
     * <p>
     * <pre>
     * 	 	如果连续失败的次数 >= maxClientConnection, 那么把client设置成不可用状态
     * </pre>
     */
    void incrErrorCount() {
        long count = errorCount.incrementAndGet();

        // 如果节点是可用状态，同时当前连续失败的次数超过允许的最大失败连接数，那么把该节点标示为不可用
        if (count >= maxClientFailedConn && state.isActive()) {
            synchronized (this) {
                count = errorCount.longValue();

                if (count >= maxClientFailedConn && state.isActive()) {
                    log.error("NettyClient unavailable Error: url=" + providerUrl.getIdentity() + " "
                            + providerUrl.getAddress());
                    state = ChannelState.INACTIVE;
                }
            }
        }
    }

    /**
     * 重置调用失败的计数 ：
     * <pre>
     * 把节点设置成可用
     * </pre>
     */
    void resetErrorCount() {
        errorCount.set(0);

        if (state.isActive()) {
            return;
        }

        synchronized (this) {
            if (state.isActive()) {
                return;
            }

            // 如果节点是inactive才进行设置，而如果是close或者uninitialized，那么直接忽略
            if (state.isInactive()) {
                long count = errorCount.longValue();

                // 过程中有其他并发更新errorCount的，因此这里需要进行一次判断
                if (count < maxClientFailedConn) {
                    state = ChannelState.ACTIVE;
                    log.info("NettyClient recover available: url=" + providerUrl.getIdentity() + " "
                            + providerUrl.getAddress());
                }
            }
        }
    }


    /**
     * 注册回调的response
     * <pre>
     * 进行最大的请求并发数的控制，如果超过NETTY_CLIENT_MAX_REQUEST的话，那么throw reject exception
     * </pre>
     *
     * @param requestId      request ID
     * @param futureResponse response future
     */
    public void registerCallback(long requestId, FutureResponse futureResponse) {
        if (this.callbackMap.size() >= RpcConstants.NETTY_CLIENT_MAX_REQUEST) {
            // reject request, prevent from OutOfMemoryError
            throw new RpcServiceException("NettyClient over of max concurrent request, drop request, url: "
                    + providerUrl.getUri() + " requestId=" + requestId, RpcErrorMsgConstant.SERVICE_REJECT);
        }

        this.callbackMap.put(requestId, futureResponse);
    }

    @Override
    public void checkHealth(Requestable request) {
        try {
            log.info("Checking health for url [{}]", providerUrl.getUri());
            // async request后，如果service is active，那么将会自动把该client设置成可用
            doRequest(request);
        } catch (Exception e) {
            log.error("Failed to check health for url [{}] with message {}", providerUrl.getUri(), e.getMessage());
        }
    }

    @Override
    public String toString() {
        return NettyClient.class.getSimpleName().concat(":").concat(getProviderUrl().getPath());
    }
}
