package org.infinity.luix.transport.netty4.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@ChannelHandler.Sharable
public class NettyServerChannelManager extends ChannelInboundHandlerAdapter {

    public static final String                         CHANNEL_MANAGER = "channelManager";
    private             ConcurrentMap<String, Channel> channels        = new ConcurrentHashMap<>();
    private             int                            maxChannel;

    public NettyServerChannelManager(int maxChannel) {
        super();
        this.maxChannel = maxChannel;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if (channels.size() >= maxChannel) {
            // 超过最大连接数限制，直接close连接
            log.warn("NettyServerChannelManage channelConnected channel size out of limit: limit={} current={}", maxChannel, channels.size());
            channel.close();
        } else {
            String channelKey = getChannelKey((InetSocketAddress) channel.localAddress(), (InetSocketAddress) channel.remoteAddress());
            channels.put(channelKey, channel);
            ctx.fireChannelRegistered();
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String channelKey = getChannelKey((InetSocketAddress) channel.localAddress(), (InetSocketAddress) channel.remoteAddress());
        channels.remove(channelKey);
        ctx.fireChannelUnregistered();
    }

    public Map<String, Channel> getChannels() {
        return channels;
    }

    /**
     * close所有的连接
     */
    public void close() {
        for (Map.Entry<String, Channel> entry : channels.entrySet()) {
            try {
                Channel channel = entry.getValue();
                if (channel != null) {
                    channel.close();
                }
            } catch (Exception e) {
                log.error("NettyServerChannelManage close channel Error: " + entry.getKey(), e);
            }
        }
    }

    /**
     * remote address + local address 作为连接的唯一标示
     *
     * @param local
     * @param remote
     * @return
     */
    private String getChannelKey(InetSocketAddress local, InetSocketAddress remote) {
        String key = "";
        if (local == null || local.getAddress() == null) {
            key += "null-";
        } else {
            key += local.getAddress().getHostAddress() + ":" + local.getPort() + "-";
        }

        if (remote == null || remote.getAddress() == null) {
            key += "null";
        } else {
            key += remote.getAddress().getHostAddress() + ":" + remote.getPort();
        }

        return key;
    }
}