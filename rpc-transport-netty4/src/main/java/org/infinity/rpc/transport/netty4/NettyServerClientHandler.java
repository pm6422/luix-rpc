package org.infinity.rpc.transport.netty4;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.RpcErrorMsgConstant;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exception.RpcServiceException;
import org.infinity.rpc.core.exchange.ExchangeContext;
import org.infinity.rpc.core.codec.Codec;
import org.infinity.rpc.core.codec.CodecUtils;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.core.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.exchange.transport.Channel;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;
import org.infinity.rpc.transport.netty4.server.NettyServer;
import org.infinity.rpc.utilities.network.NetworkUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @todo: NettyChannelHandler
 */
@Slf4j
public class NettyServerClientHandler extends ChannelDuplexHandler {
    private ThreadPoolExecutor threadPoolExecutor;
    private MessageHandler     messageHandler;
    private Channel            channel;
    private Codec              codec;

    public NettyServerClientHandler(Channel channel, MessageHandler messageHandler) {
        this.channel = channel;
        this.messageHandler = messageHandler;
        codec = Codec.getInstance(channel.getProviderUrl().getOption(Url.PARAM_CODEC, Url.PARAM_CODEC_DEFAULT_VALUE));
    }

    public NettyServerClientHandler(Channel channel, MessageHandler messageHandler, ThreadPoolExecutor threadPoolExecutor) {
        this.channel = channel;
        this.messageHandler = messageHandler;
        this.threadPoolExecutor = threadPoolExecutor;
        codec = Codec.getInstance(channel.getProviderUrl().getOption(Url.PARAM_CODEC, Url.PARAM_CODEC_DEFAULT_VALUE));
    }

    private String getRemoteIp(ChannelHandlerContext ctx) {
        String ip = "";
        SocketAddress remote = ctx.channel().remoteAddress();
        if (remote != null) {
            try {
                ip = ((InetSocketAddress) remote).getAddress().getHostAddress();
            } catch (Exception e) {
                log.warn("get remoteIp error! default will use. msg:{}, remote:{}", e.getMessage(), remote.toString());
            }
        }
        return ip;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (msg instanceof NettyMessage) {
            if (threadPoolExecutor != null) {
                try {
                    threadPoolExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            // Step2: receive and decode request on server side
                            processRequestOrResponseMsg(ctx, ((NettyMessage) msg));
                        }
                    });
                } catch (RejectedExecutionException rejectException) {
                    if (((NettyMessage) msg).isRequest()) {
                        rejectMessage(ctx, (NettyMessage) msg);
                    } else {
                        log.warn("process thread pool is full, run in io thread, active={} poolSize={} corePoolSize={} maxPoolSize={} taskCount={} requestId={}",
                                threadPoolExecutor.getActiveCount(), threadPoolExecutor.getPoolSize(), threadPoolExecutor.getCorePoolSize(),
                                threadPoolExecutor.getMaximumPoolSize(), threadPoolExecutor.getTaskCount(), ((NettyMessage) msg).getRequestId());
                        processRequestOrResponseMsg(ctx, (NettyMessage) msg);
                    }
                }
            } else {
                // Step4: receive and decode response on client side
                processRequestOrResponseMsg(ctx, (NettyMessage) msg);
            }
        } else {
            log.error("NettyChannelHandler messageReceived type not support: class=" + msg.getClass());
            throw new RpcFrameworkException("NettyChannelHandler messageReceived type not support: class=" + msg.getClass());
        }
    }

    private void rejectMessage(ChannelHandlerContext ctx, NettyMessage msg) {
        if (msg.isRequest()) {
            returnResponse(ctx, RpcFrameworkUtils.buildErrorResponse((Requestable) msg, new RpcServiceException("process thread pool is full, reject by server: " + ctx.channel().localAddress(), RpcErrorMsgConstant.SERVICE_REJECT)));

            log.error("process thread pool is full, reject, active={} poolSize={} corePoolSize={} maxPoolSize={} taskCount={} requestId={}",
                    threadPoolExecutor.getActiveCount(), threadPoolExecutor.getPoolSize(), threadPoolExecutor.getCorePoolSize(),
                    threadPoolExecutor.getMaximumPoolSize(), threadPoolExecutor.getTaskCount(), msg.getRequestId());
            if (channel instanceof NettyServer) {
                ((NettyServer) channel).getRejectCounter().incrementAndGet();
            }
        }
    }

    private void processRequestOrResponseMsg(ChannelHandlerContext ctx, NettyMessage msg) {
        long startTime = System.currentTimeMillis();
        String remoteIp = getRemoteIp(ctx);
        Object decodedObj;
        try {
            decodedObj = codec.decode(channel, remoteIp, msg.getData());
        } catch (Exception e) {
            log.error("NettyDecoder decode fail! requestid" + msg.getRequestId() + ", size:" + msg.getData().length + ", ip:" + remoteIp + ", e:" + e.getMessage());
            Responseable response = RpcFrameworkUtils.buildErrorResponse(msg.getRequestId(), msg.getVersion().getVersion(), e);
            if (msg.isRequest()) {
                // Step3: directly return response on server side
                returnResponse(ctx, response);
            } else {
                // Process response
                processResponse(response);
            }
            return;
        }

        if (decodedObj instanceof Requestable) {
            // Process request
            RpcFrameworkUtils.logEvent((Requestable) decodedObj, RpcConstants.TRACE_SRECEIVE, msg.getStartTime());
            RpcFrameworkUtils.logEvent((Requestable) decodedObj, RpcConstants.TRACE_SEXECUTOR_START, startTime);
            RpcFrameworkUtils.logEvent((Requestable) decodedObj, RpcConstants.TRACE_SDECODE);
            processRequest(ctx, (Requestable) decodedObj);
        } else if (decodedObj instanceof Responseable) {
            // Process response
            RpcFrameworkUtils.logEvent((Responseable) decodedObj, RpcConstants.TRACE_CRECEIVE, msg.getStartTime());
            RpcFrameworkUtils.logEvent((Responseable) decodedObj, RpcConstants.TRACE_CDECODE);
            processResponse(decodedObj);
        }
    }

    private void processRequest(final ChannelHandlerContext ctx, final Requestable request) {
        request.addOption(Url.PARAM_HOST, NetworkUtils.getHostName(ctx.channel().remoteAddress()));
        final long processStartTime = System.currentTimeMillis();
        try {
            ExchangeContext.initialize(request);
            Object result;
            try {
                result = messageHandler.handle(channel, request);
            } catch (Exception e) {
                log.error("NettyChannelHandler processRequest fail! request:" + request, e);
                result = RpcFrameworkUtils.buildErrorResponse(request, new RpcServiceException("process request fail. errmsg:" + e.getMessage()));
            }
            if (result instanceof Responseable) {
                RpcFrameworkUtils.logEvent((Responseable) result, RpcConstants.TRACE_PROCESS);
            }
            final RpcResponse response;
            if (result instanceof RpcResponse) {
                response = (RpcResponse) result;
                response.setProtocolVersion(request.getProtocolVersion());
            } else {
                response = new RpcResponse(result);
            }
            response.setRequestId(request.getRequestId());
            response.setElapsedTime(System.currentTimeMillis() - processStartTime);

            // Step3: encode and return response on server side
            ChannelFuture channelFuture = returnResponse(ctx, response);
            if (channelFuture != null) {
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        RpcFrameworkUtils.logEvent(response, RpcConstants.TRACE_SSEND, System.currentTimeMillis());
                        response.onFinish();
                    }
                });
            }
        } finally {
            ExchangeContext.destroy();
        }
    }

    private ChannelFuture returnResponse(ChannelHandlerContext ctx, Responseable response) {
        // Encode the response
        byte[] msg = CodecUtils.encodeObjectToBytes(channel, codec, response);
        response.addOption(RpcConstants.CONTENT_LENGTH, String.valueOf(msg.length));
        if (ctx.channel().isActive()) {
            return ctx.channel().writeAndFlush(msg);
        }
        return null;
    }

    private void processResponse(Object msg) {
        messageHandler.handle(channel, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("NettyChannelHandler channelActive: remote={} local={}", ctx.channel().remoteAddress(), ctx.channel().localAddress());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("NettyChannelHandler channelInactive: remote={} local={}", ctx.channel().remoteAddress(), ctx.channel().localAddress());
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("NettyChannelHandler exceptionCaught: remote={} local={} event={}", ctx.channel().remoteAddress(), ctx.channel().localAddress(), cause.getMessage(), cause);
        ctx.channel().close();
    }
}
