package org.infinity.rpc.transport.netty4;

import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.codec.Codec;
import org.infinity.rpc.core.exchange.codec.CodecUtils;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Future;
import org.infinity.rpc.core.exchange.response.FutureListener;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.RpcResponseFuture;
import org.infinity.rpc.core.exchange.response.impl.DefaultResponseFuture;
import org.infinity.rpc.core.exchange.transport.Channel;
import org.infinity.rpc.core.exchange.transport.constants.ChannelState;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.url.UrlParam;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;
import org.infinity.rpc.utilities.spi.ServiceLoader;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class NettyChannel implements Channel {
    private volatile ChannelState             state         = ChannelState.UNINITIALIZED;
    private          NettyClient              nettyClient;
    private          io.netty.channel.Channel channel       = null;
    private          InetSocketAddress        remoteAddress = null;
    private          InetSocketAddress        localAddress  = null;
    private          ReentrantLock            lock          = new ReentrantLock();
    private          Codec                    codec;

    public NettyChannel(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
        this.remoteAddress = new InetSocketAddress(nettyClient.getUrl().getHost(), nettyClient.getUrl().getPort());
        codec = ServiceLoader.forClass(Codec.class).load(nettyClient.getUrl().getParameter(Url.PARAM_CODEC, Url.PARAM_CODEC_DEFAULT_VALUE));
    }

    @Override
    public Responseable request(Requestable request) {
        int timeout = nettyClient.getUrl().getMethodParameter(request.getMethodName(), request.getParameterTypeList(), UrlParam.requestTimeout.getName(), UrlParam.requestTimeout.getIntValue());
        if (timeout <= 0) {
            throw new RpcFrameworkException("NettyClient init Error: timeout(" + timeout + ") <= 0 is forbid.", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        RpcResponseFuture response = new DefaultResponseFuture(request, timeout, this.nettyClient.getUrl());
        this.nettyClient.registerCallback(request.getRequestId(), response);
        byte[] msg = CodecUtils.encodeObjectToBytes(this, codec, request);
        ChannelFuture writeFuture = this.channel.writeAndFlush(msg);
        boolean result = writeFuture.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);

        if (result && writeFuture.isSuccess()) {
            RpcFrameworkUtils.logEvent(request, RpcConstants.TRACE_CSEND, System.currentTimeMillis());
            response.addListener(new FutureListener() {
                @Override
                public void operationComplete(Future future) throws Exception {
                    if (future.isSuccess() || (future.isDone() && ExceptionUtils.isBizException(future.getException()))) {
                        // 成功的调用
                        nettyClient.resetErrorCount();
                    } else {
                        // 失败的调用
                        nettyClient.incrErrorCount();
                    }
                }
            });
            return response;
        }

        writeFuture.cancel(true);
        response = this.nettyClient.removeCallback(request.getRequestId());
        if (response != null) {
            response.cancel();
        }
        // 失败的调用
        nettyClient.incrErrorCount();

        if (writeFuture.cause() != null) {
            throw new RpcServiceException("NettyChannel send request to server Error: url="
                    + nettyClient.getUrl().getUri() + " local=" + localAddress + " "
                    + request, writeFuture.cause());
        } else {
            throw new RpcServiceException("NettyChannel send request to server Timeout: url="
                    + nettyClient.getUrl().getUri() + " local=" + localAddress + " "
                    + request);
        }
    }

    @Override
    public boolean open() {
        return false;
    }

    @Override
    public void close() {
        close(0);
    }

    @Override
    public void close(int timeout) {
        try {
            state = ChannelState.CLOSED;
            if (channel != null) {
                channel.close();
            }
        } catch (Exception e) {
            log.error("NettyChannel close Error: " + nettyClient.getUrl().getUri() + " local=" + localAddress, e);
        }
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public boolean isClosed() {
        return state.isClosed();
    }

    @Override
    public boolean isActive() {
        return state.isActive() && channel != null && channel.isActive();
    }

    @Override
    public Url getUrl() {
        return nettyClient.getUrl();
    }

    public void reconnect() {
        state = ChannelState.INITIALIZED;
    }

    public boolean isReconnect() {
        return state.isInitialized();
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
