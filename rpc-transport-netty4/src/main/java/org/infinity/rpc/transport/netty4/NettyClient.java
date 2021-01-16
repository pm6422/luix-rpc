package org.infinity.rpc.transport.netty4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.config.spring.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.ExchangeContext;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.ResponseFuture;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.impl.RpcResponse;
import org.infinity.rpc.core.exchange.transport.AbstractSharedPoolClient;
import org.infinity.rpc.core.exchange.transport.Channel;
import org.infinity.rpc.core.exchange.transport.SharedObjectFactory;
import org.infinity.rpc.core.exchange.transport.constants.ChannelState;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * toto: implements StatisticCallback
 */
@Slf4j
public class NettyClient extends AbstractSharedPoolClient {
    private static final NioEventLoopGroup         NIO_EVENT_LOOP_GROUP = new NioEventLoopGroup();
    /**
     * 回收过期任务
     */
    private static final ScheduledExecutorService  scheduledExecutor    = Executors.newScheduledThreadPool(1);
    /**
     * 异步的request，需要注册callback future
     * 触发remove的操作有： 1) service的返回结果处理。 2) timeout thread cancel
     */
    protected            Map<Long, ResponseFuture> callbackMap          = new ConcurrentHashMap<>();
    /**
     * 连续失败次数
     */
    private              AtomicLong                errorCount           = new AtomicLong(0);
    private              ScheduledFuture<?>        timeMonitorFuture;
    private              Bootstrap                 bootstrap;
    private              int                       maxClientConnection;

    public NettyClient(Url providerUrl) {
        super(providerUrl);
        maxClientConnection = providerUrl.getIntParameter(Url.PARAM_MAX_CLIENT_CONNECTION, Url.PARAM_MAX_CLIENT_CONNECTION_DEFAULT_VALUE);
        Validate.isTrue(maxClientConnection > 0, "maxClientConnection must be a positive number!");

        timeMonitorFuture = scheduledExecutor.scheduleWithFixedDelay(
                new TimeoutMonitor("timeout-monitor-" + providerUrl.getHost() + "-" + providerUrl.getPort()),
                RpcConstants.NETTY_TIMEOUT_TIMER_PERIOD, RpcConstants.NETTY_TIMEOUT_TIMER_PERIOD,
                TimeUnit.MILLISECONDS);
    }

    @Override
    protected SharedObjectFactory createChannelFactory() {
        return new NettyChannelFactory(this);
    }

    @Override
    public Responseable request(Requestable request) {
        if (!isActive()) {
            throw new RpcServiceException("NettyChannel is unavailable: url=" + providerUrl.getUri() + request);
        }
        boolean isAsync = false;
        Object async = ExchangeContext.getInstance().getAttribute(RpcConstants.ASYNC_SUFFIX);
        if (async != null && async instanceof Boolean) {
            isAsync = (Boolean) async;
        }
        return request(request, isAsync);
    }

    private Responseable request(Requestable request, boolean async) {
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

            // async request
            response = channel.request(request);
        } catch (Exception e) {
            log.error("NettyClient request Error: url=" + providerUrl.getUri() + " " + request + ", " + e.getMessage());

            if (e instanceof RpcAbstractException) {
                throw (RpcAbstractException) e;
            } else {
                throw new RpcServiceException("NettyClient request Error: url=" + providerUrl.getUri() + " " + request, e);
            }
        }

        // aysnc or sync result
        response = asyncResponse(response, async);
        return response;
    }

    /**
     * 如果async是false，那么同步获取response的数据
     *
     * @param response
     * @param async
     * @return
     */
    private Responseable asyncResponse(Responseable response, boolean async) {
        if (async || !(response instanceof ResponseFuture)) {
            return response;
        }
        return new RpcResponse(response);
    }

    @Override
    public synchronized boolean open() {
        if (isActive()) {
            return true;
        }

        bootstrap = new Bootstrap();
        int timeout = getProviderUrl().getIntParameter(Url.PARAM_CONNECT_TIMEOUT, Url.PARAM_CONNECT_TIMEOUT_DEFAULT_VALUE);
        if (timeout <= 0) {
            throw new RpcFrameworkException("NettyClient init Error: timeout(" + timeout + ") <= 0 is forbid.", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        // 最大响应包限制
        final int maxContentLength = providerUrl.getIntParameter(Url.PARAM_MAX_CONTENT_LENGTH, Url.PARAM_MAX_CONTENT_LENGTH_DEFAULT_VALUE);
        bootstrap.group(NIO_EVENT_LOOP_GROUP)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new NettyDecoder(codec, NettyClient.this, maxContentLength));
                        pipeline.addLast("encoder", new NettyEncoder());
                        pipeline.addLast("handler", new NettyChannelHandler(NettyClient.this, new MessageHandler() {
                            @Override
                            public Object handle(Channel channel, Object message) {
                                Responseable response = (Responseable) message;
                                ResponseFuture responseFuture = NettyClient.this.removeCallback(response.getRequestId());

                                if (responseFuture == null) {
                                    log.warn("NettyClient has response from server, but responseFuture not exist, requestId={}", response.getRequestId());
                                    return null;
                                }
                                if (response.getException() != null) {
                                    responseFuture.onFailure(response);
                                } else {
                                    responseFuture.onSuccess(response);
                                }
                                return null;
                            }
                        }));
                    }
                });

        // 初始化连接池
        initPool();

        log.info("NettyClient finish Open: url={}", providerUrl);

        // 注册统计回调
//        StatsUtil.registryStatisticCallback(this);

        // 设置可用状态
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
        timeMonitorFuture.cancel(true);
        // 清空callback
        callbackMap.clear();
        // 关闭client持有的channel
        closeAllChannels();
        // 解除统计回调的注册
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

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public ResponseFuture removeCallback(long requestId) {
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

        // 如果节点是可用状态，同时当前连续失败的次数超过连接数，那么把该节点标示为不可用
        if (count >= maxClientConnection && state.isActive()) {
            synchronized (this) {
                count = errorCount.longValue();

                if (count >= maxClientConnection && state.isActive()) {
                    log.error("NettyClient unavailable Error: url=" + providerUrl.getIdentity() + " "
                            + providerUrl.getServerPortStr());
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

            // 如果节点是unalive才进行设置，而如果是 close 或者 uninit，那么直接忽略
            if (state.isInactive()) {
                long count = errorCount.longValue();

                // 过程中有其他并发更新errorCount的，因此这里需要进行一次判断
                if (count < maxClientConnection) {
                    state = ChannelState.ACTIVE;
                    log.info("NettyClient recover available: url=" + providerUrl.getIdentity() + " "
                            + providerUrl.getServerPortStr());
                }
            }
        }
    }


    /**
     * 注册回调的resposne
     * <pre>
     * 进行最大的请求并发数的控制，如果超过NETTY_CLIENT_MAX_REQUEST的话，那么throw reject exception
     * </pre>
     *
     * @param requestId
     * @param responseFuture
     * @throws RpcServiceException
     */
    public void registerCallback(long requestId, ResponseFuture responseFuture) {
        if (this.callbackMap.size() >= RpcConstants.NETTY_CLIENT_MAX_REQUEST) {
            // reject request, prevent from OutOfMemoryError
            throw new RpcServiceException("NettyClient over of max concurrent request, drop request, url: "
                    + providerUrl.getUri() + " requestId=" + requestId, RpcErrorMsgConstant.SERVICE_REJECT);
        }

        this.callbackMap.put(requestId, responseFuture);
    }

    @Override
    public void checkHealth(Requestable request) {
        if (state.isUninitialized() || state.isClosed()) {
            // 如果节点还没有初始化或者节点已经被close掉了，那么heartbeat也不需要进行了
            log.warn("NettyClient heartbeat Error: state={} url={}", state.name(), providerUrl.getUri());
            return;
        }

        log.info("Checking health of url [{}]", providerUrl.getUri());
        try {
            // async request后，如果service is
            // available，那么将会自动把该client设置成可用
            request(request, true);
        } catch (Exception e) {
            log.error("NettyClient heartbeat Error: url={}, {}", providerUrl.getUri(), e.getMessage());
        }
    }

    @Override
    public String toString() {
        return NettyClient.class.getSimpleName().concat(":").concat(getProviderUrl().getPath());
    }

    /**
     * 回收超时任务
     */
    class TimeoutMonitor implements Runnable {
        private String name;

        public TimeoutMonitor(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            //todo:
//            long currentTime = System.currentTimeMillis();
//            for (Map.Entry<Long, RpcResponseFuture> entry : callbackMap.entrySet()) {
//                try {
//                    RpcResponseFuture future = entry.getValue();
//                    if (future.getCreateTime() + future.getProcessingTimeout() < currentTime) {
//                        // timeout: remove from callback list, and then cancel
//                        removeCallback(entry.getKey());
//                        future.cancel();
//                    }
//                } catch (Exception e) {
//                    log.error(name + " clear timeout future Error: uri=" + url.getUri() + " requestId=" + entry.getKey(), e);
//                }
//            }
        }
    }
}
