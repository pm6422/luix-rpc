package org.infinity.rpc.transport.netty4.client;

import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.codec.Codec;
import org.infinity.rpc.core.codec.CodecUtils;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.ExceptionUtils;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.exchange.Channel;
import org.infinity.rpc.core.exchange.constants.ChannelState;
import org.infinity.rpc.core.server.response.FutureResponse;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcFutureResponse;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC;
import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC_VAL_V1;
import static org.infinity.rpc.core.constant.RegistryConstants.CONNECT_TIMEOUT;
import static org.infinity.rpc.core.constant.RegistryConstants.CONNECT_TIMEOUT_VAL_DEFAULT;
import static org.infinity.rpc.core.constant.ServiceConstants.REQUEST_TIMEOUT;
import static org.infinity.rpc.core.constant.ServiceConstants.REQUEST_TIMEOUT_VAL_DEFAULT;

@Slf4j
public class NettyChannel implements Channel {
    private volatile ChannelState             state = ChannelState.UNINITIALIZED;
    private final    NettyClient              nettyClient;
    private          io.netty.channel.Channel channel;
    private final    InetSocketAddress        remoteAddress;
    private          InetSocketAddress        localAddress;
    private final    ReentrantLock            lock  = new ReentrantLock();
    private final    Codec                    codec;

    public NettyChannel(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
        this.remoteAddress = new InetSocketAddress(nettyClient.getProviderUrl().getHost(), nettyClient.getProviderUrl().getPort());
        codec = Codec.getInstance(nettyClient.getProviderUrl().getOption(CODEC, CODEC_VAL_V1));
    }

    @Override
    public Responseable request(Requestable request) {
        int timeout;
        if (StringUtils.isNotEmpty(request.getOption(REQUEST_TIMEOUT))) {
            timeout = request.getIntOption(REQUEST_TIMEOUT);
        } else {
            // Get method level parameter value
            timeout = nettyClient.getProviderUrl()
                    .getMethodParameter(request.getMethodName(), request.getMethodParameters(),
                            REQUEST_TIMEOUT, REQUEST_TIMEOUT_VAL_DEFAULT);
        }
        // All requests are handled asynchronously
        FutureResponse response = new RpcFutureResponse(request, timeout, this.nettyClient.getProviderUrl());
        this.nettyClient.registerCallback(request.getRequestId(), response);
        byte[] msg = CodecUtils.encodeObjectToBytes(this, codec, request);
        // Step1: encode and send request on client side
        ChannelFuture writeFuture = this.channel.writeAndFlush(msg);
        boolean result = writeFuture.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);

        if (result && writeFuture.isSuccess()) {
            RpcFrameworkUtils.logEvent(request, RpcConstants.TRACE_CSEND, System.currentTimeMillis());
            response.addListener(future -> {
                if (future.isSuccess() ||
                        (future.isDone() && ExceptionUtils.isBizException(future.getException()))) {
                    // 成功的调用
                    // Step5: get response on client side
                    nettyClient.resetErrorCount();
                } else {
                    // 失败的调用
                    // Step5: get response on client side
                    nettyClient.incrErrorCount();
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
            throw new RpcFrameworkException("NettyChannel send request to server Error: url="
                    + nettyClient.getProviderUrl().getUri() + " local=" + localAddress + " "
                    + request, writeFuture.cause());
        } else {
            throw new RpcFrameworkException("NettyChannel send request to server Timeout: url="
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
            int timeout = nettyClient.getProviderUrl().getIntOption(CONNECT_TIMEOUT, CONNECT_TIMEOUT_VAL_DEFAULT);
            if (timeout <= 0) {
                throw new RpcFrameworkException("NettyClient init Error: timeout(" + timeout + ") <= 0 is forbid.");
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
                throw new RpcFrameworkException("NettyChannel failed to connect to server, url: " + nettyClient.getProviderUrl().getUri() + ", result: " + result + ", success: " + success + ", connected: " + connected, channelFuture.cause());
            } else {
                channelFuture.cancel(true);
                throw new RpcFrameworkException("NettyChannel connect to server timeout url: " + nettyClient.getProviderUrl().getUri() + ", cost: " + (System.currentTimeMillis() - start) + ", result: " + result + ", success: " + success + ", connected: " + connected);
            }
        } catch (RpcFrameworkException e) {
            throw e;
        } catch (Exception e) {
            if (channelFuture != null) {
                channelFuture.channel().close();
            }
            throw new RpcFrameworkException("NettyChannel failed to connect to server, url: " + nettyClient.getProviderUrl().getUri(), e);
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
    public ChannelState getState() {
        return state;
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
