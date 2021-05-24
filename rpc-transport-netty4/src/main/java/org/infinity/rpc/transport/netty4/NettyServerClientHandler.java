package org.infinity.rpc.transport.netty4;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.core.client.request.Requestable;
import org.infinity.rpc.core.codec.Codec;
import org.infinity.rpc.core.codec.CodecUtils;
import org.infinity.rpc.core.constant.RpcConstants;
import org.infinity.rpc.core.exception.impl.RpcFrameworkException;
import org.infinity.rpc.core.exchange.Channel;
import org.infinity.rpc.core.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.server.response.Responseable;
import org.infinity.rpc.core.server.response.impl.RpcResponse;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.RpcFrameworkUtils;
import org.infinity.rpc.core.utils.RpcRequestIdHolder;
import org.infinity.rpc.transport.netty4.server.NettyServer;
import org.infinity.rpc.utilities.network.AddressUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC;
import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC_VAL_DEFAULT;

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
        codec = Codec.getInstance(channel.getProviderUrl().getOption(CODEC, CODEC_VAL_DEFAULT));
    }

    public NettyServerClientHandler(Channel channel, MessageHandler messageHandler, ThreadPoolExecutor threadPoolExecutor) {
        this.channel = channel;
        this.messageHandler = messageHandler;
        this.threadPoolExecutor = threadPoolExecutor;
        codec = Codec.getInstance(channel.getProviderUrl().getOption(CODEC, CODEC_VAL_DEFAULT));
    }

    private String getRemoteIp(ChannelHandlerContext ctx) {
        String ip = "";
        SocketAddress remote = ctx.channel().remoteAddress();
        if (remote != null) {
            try {
                ip = ((InetSocketAddress) remote).getAddress().getHostAddress();
            } catch (Exception e) {
                log.error("Failed to get the IP", e);
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
                        log.warn("Current processing thread pool is full, active: {}, poolSize: {}, corePoolSize: {}, " +
                                        "maxPoolSize: {}, taskCount: {}, requestId: {}",
                                threadPoolExecutor.getActiveCount(), threadPoolExecutor.getPoolSize(),
                                threadPoolExecutor.getCorePoolSize(), threadPoolExecutor.getMaximumPoolSize(),
                                threadPoolExecutor.getTaskCount(), ((NettyMessage) msg).getRequestId());
                        processRequestOrResponseMsg(ctx, (NettyMessage) msg);
                    }
                }
            } else {
                // Step4: receive and decode response on client side
                processRequestOrResponseMsg(ctx, (NettyMessage) msg);
            }
        } else {
            throw new RpcFrameworkException("Received unsupported message type [" + msg.getClass() + "]");
        }
    }

    private void rejectMessage(ChannelHandlerContext ctx, NettyMessage msg) {
        if (msg.isRequest()) {
            returnResponse(ctx, RpcFrameworkUtils.buildErrorResponse((Requestable) msg,
                    new RpcFrameworkException("Reject the request for no active thread on server [" + ctx.channel().localAddress() + "]")));
            log.error("Rejected message for current processing thread pool is full, active: {}, poolSize: {}, " +
                            "corePoolSize: {}, maxPoolSize: {}, taskCount: {}, requestId: {}",
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
            log.error("Failed to decode message with message ID [" + msg.getRequestId() + "] and remote IP [" + remoteIp + "]", e);
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
        // Used by access log output
        request.addOption(Url.PARAM_HOST, AddressUtils.getHostName(ctx.channel().remoteAddress()));
        final long processStartTime = System.currentTimeMillis();
        try {
            // Store request id on server side
            RpcRequestIdHolder.setRequestId(request.getRequestId());
            Object result;
            try {
                result = messageHandler.handle(channel, request);
            } catch (Exception e) {
                log.error("Failed to process request " + request, e);
                result = RpcFrameworkUtils.buildErrorResponse(request, new RpcFrameworkException("Failed to process request with error [" + e.getMessage() + "]"));
            }
            if (result instanceof Responseable) {
                RpcFrameworkUtils.logEvent((Responseable) result, RpcConstants.TRACE_PROCESS);
            }
            final RpcResponse response;
            if (result instanceof RpcResponse) {
                response = (RpcResponse) result;
                response.setProtocolVersion(request.getProtocolVersion());
            } else {
                response = RpcResponse.of(result);
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
            RpcRequestIdHolder.destroy();
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
        log.info("Detected active channel with remoteAddress [{}] and localAddress [{}]",
                ctx.channel().remoteAddress(), ctx.channel().localAddress());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Detected inactive channel with remoteAddress [{}] and localAddress [{}]",
                ctx.channel().remoteAddress(), ctx.channel().localAddress());
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Caught exception with remoteAddress [{}], localAddress [{}] and event [{}]",
                ctx.channel().remoteAddress(), ctx.channel().localAddress(), cause.getMessage(), cause);
        ctx.channel().close();
    }
}
