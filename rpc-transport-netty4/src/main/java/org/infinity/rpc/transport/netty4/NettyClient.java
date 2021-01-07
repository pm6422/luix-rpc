package org.infinity.rpc.transport.netty4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.RpcResponseFuture;
import org.infinity.rpc.core.exchange.transport.AbstractSharedPoolClient;
import org.infinity.rpc.core.exchange.transport.SharedObjectFactory;
import org.infinity.rpc.core.exchange.transport.constants.ChannelState;
import org.infinity.rpc.core.exchange.transport.exception.TransmissionException;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.url.UrlParam;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * toto: implements StatisticCallback
 */
@Slf4j
public class NettyClient extends AbstractSharedPoolClient {
    private static final NioEventLoopGroup            NIO_EVENT_LOOP_GROUP = new NioEventLoopGroup();
    /**
     * 回收过期任务
     */
    private static       ScheduledExecutorService     scheduledExecutor    = Executors.newScheduledThreadPool(1);
    /**
     * 异步的request，需要注册callback future
     * 触发remove的操作有： 1) service的返回结果处理。 2) timeout thread cancel
     */
    protected            Map<Long, RpcResponseFuture> callbackMap          = new ConcurrentHashMap<>();
    /**
     * 连续失败次数
     */
    private              AtomicLong                   errorCount           = new AtomicLong(0);
    private              ScheduledFuture<?>           timeMonitorFuture;
    private              Bootstrap                    bootstrap;
    private              int                          maxClientConnection;

    public NettyClient(Url url) {
        super(url);
        maxClientConnection = url.getIntParameter(UrlParam.maxClientConnection.getName(), UrlParam.maxClientConnection.getIntValue());
        Validate.isTrue(maxClientConnection > 0, "maxClientConnection must be a positive number!");
        timeMonitorFuture = scheduledExecutor.scheduleWithFixedDelay(
                new TimeoutMonitor("timeout-monitor-" + url.getHost() + "-" + url.getPort()),
                RpcConstants.NETTY_TIMEOUT_TIMER_PERIOD, RpcConstants.NETTY_TIMEOUT_TIMER_PERIOD,
                TimeUnit.MILLISECONDS);
    }

    @Override
    protected SharedObjectFactory createChannelFactory() {
        return new NettyChannelFactory(this);
    }

    @Override
    public Responseable request(Requestable request) throws TransmissionException {
        return null;
    }

    @Override
    public boolean open() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public Url getUrl() {
        return null;
    }

    public RpcResponseFuture removeCallback(long requestId) {
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
                    log.error("NettyClient unavailable Error: url=" + url.getIdentity() + " "
                            + url.getServerPortStr());
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
                    log.info("NettyClient recover available: url=" + url.getIdentity() + " "
                            + url.getServerPortStr());
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
    public void registerCallback(long requestId, RpcResponseFuture responseFuture) {
        if (this.callbackMap.size() >= RpcConstants.NETTY_CLIENT_MAX_REQUEST) {
            // reject request, prevent from OutOfMemoryError
            throw new RpcServiceException("NettyClient over of max concurrent request, drop request, url: "
                    + url.getUri() + " requestId=" + requestId, RpcErrorMsgConstant.SERVICE_REJECT);
        }

        this.callbackMap.put(requestId, responseFuture);
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
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<Long, RpcResponseFuture> entry : callbackMap.entrySet()) {
                try {
                    RpcResponseFuture future = entry.getValue();
                    if (future.getCreateTime() + future.getProcessingTimeout() < currentTime) {
                        // timeout: remove from callback list, and then cancel
                        removeCallback(entry.getKey());
                        future.cancel();
                    }
                } catch (Exception e) {
                    log.error(name + " clear timeout future Error: uri=" + url.getUri() + " requestId=" + entry.getKey(), e);
                }
            }
        }
    }
}
