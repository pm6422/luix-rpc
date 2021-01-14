package org.infinity.rpc.transport.netty4;

import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.constant.ServiceConstants;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.codec.Codec;
import org.infinity.rpc.core.exchange.codec.CodecUtils;
import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Future;
import org.infinity.rpc.core.exchange.response.FutureListener;
import org.infinity.rpc.core.exchange.response.ResponseFuture;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.response.impl.DefaultResponseFuture;
import org.infinity.rpc.core.exchange.transport.Channel;
import org.infinity.rpc.core.exchange.transport.constants.ChannelState;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class NettyChannel implements Channel {
    private volatile ChannelState             state = ChannelState.UNINITIALIZED;
    private          NettyClient              nettyClient;
    private          io.netty.channel.Channel channel;
    private          InetSocketAddress        remoteAddress;
    private          InetSocketAddress        localAddress;
    private          ReentrantLock            lock  = new ReentrantLock();
    private          Codec                    codec;

    public NettyChannel(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
        this.remoteAddress = new InetSocketAddress(nettyClient.getProviderUrl().getHost(), nettyClient.getProviderUrl().getPort());
        codec = Codec.getInstance(nettyClient.getProviderUrl().getParameter(Url.PARAM_CODEC, Url.PARAM_CODEC_DEFAULT_VALUE));
    }

    @Override
    public Responseable request(Requestable request) {
        // Get method level parameter value
        int timeout = nettyClient.getProviderUrl()
                .getMethodParameter(request.getMethodName(), request.getMethodParameters(),
                        Url.PARAM_REQUEST_TIMEOUT, ServiceConstants.REQUEST_TIMEOUT_DEFAULT_VALUE);
        ResponseFuture response = new DefaultResponseFuture(request, timeout, this.nettyClient.getProviderUrl());
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
                    + nettyClient.getProviderUrl().getUri() + " local=" + localAddress + " "
                    + request, writeFuture.cause());
        } else {
            throw new RpcServiceException("NettyChannel send request to server Timeout: url="
                    + nettyClient.getProviderUrl().getUri() + " local=" + localAddress + " "
                    + request);
        }
    }

    @Override
    public synchronized boolean open() {
        if (isActive()) {
            log.warn("the channel already open, local: " + localAddress + " remote: " + remoteAddress + " url: " + nettyClient.getProviderUrl().getUri());
            return true;
        }

        ChannelFuture channelFuture = null;
        try {
            long start = System.currentTimeMillis();
            channelFuture = nettyClient.getBootstrap().connect(remoteAddress);
            int timeout = nettyClient.getProviderUrl().getIntParameter(Url.PARAM_CONNECT_TIMEOUT, Url.PARAM_CONNECT_TIMEOUT_DEFAULT_VALUE);
            if (timeout <= 0) {
                throw new RpcFrameworkException("NettyClient init Error: timeout(" + timeout + ") <= 0 is forbid.", RpcErrorMsgConstant.FRAMEWORK_INIT_ERROR);
            }
            // 不去依赖于connectTimeout
            boolean result = channelFuture.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);
            boolean success = channelFuture.isSuccess();

            if (result && success) {
                channel = channelFuture.channel();
                if (channel.localAddress() != null && channel.localAddress() instanceof InetSocketAddress) {
                    localAddress = (InetSocketAddress) channel.localAddress();
                }
                state = ChannelState.ACTIVE;
                return true;
            }
            boolean connected = false;
            if (channelFuture.channel() != null) {
                connected = channelFuture.channel().isActive();
            }

            if (channelFuture.cause() != null) {
                channelFuture.cancel(true);
                throw new RpcServiceException("NettyChannel failed to connect to server, url: " + nettyClient.getProviderUrl().getUri() + ", result: " + result + ", success: " + success + ", connected: " + connected, channelFuture.cause());
            } else {
                channelFuture.cancel(true);
                throw new RpcServiceException("NettyChannel connect to server timeout url: " + nettyClient.getProviderUrl().getUri() + ", cost: " + (System.currentTimeMillis() - start) + ", result: " + result + ", success: " + success + ", connected: " + connected);
            }
        } catch (RpcServiceException e) {
            throw e;
        } catch (Exception e) {
            if (channelFuture != null) {
                channelFuture.channel().close();
            }
            throw new RpcServiceException("NettyChannel failed to connect to server, url: " + nettyClient.getProviderUrl().getUri(), e);
        } finally {
            if (!state.isActive()) {
                nettyClient.incrErrorCount();
            }
        }
    }

    @Override
    public synchronized void close() {
        close(0);
    }

    @Override
    public synchronized void close(int timeout) {
        try {
            state = ChannelState.CLOSED;
            if (channel != null) {
                channel.close();
            }
        } catch (Exception e) {
            log.error("NettyChannel close Error: " + nettyClient.getProviderUrl().getUri() + " local=" + localAddress, e);
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
    public Url getProviderUrl() {
        return nettyClient.getProviderUrl();
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
