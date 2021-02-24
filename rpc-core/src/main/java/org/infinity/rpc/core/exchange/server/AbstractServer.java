package org.infinity.rpc.core.exchange.server;

import org.infinity.rpc.core.codec.Codec;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.Channel;
import org.infinity.rpc.core.exchange.constants.ChannelState;
import org.infinity.rpc.core.url.Url;

import java.net.InetSocketAddress;
import java.util.Collection;

import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC;
import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC_VAL_DEFAULT;

public abstract class AbstractServer implements Server {
    protected InetSocketAddress localAddress;
    protected InetSocketAddress remoteAddress;

    protected Url   providerUrl;
    protected Codec codec;

    protected volatile ChannelState state = ChannelState.UNINITIALIZED;


    public AbstractServer() {
    }

    public AbstractServer(Url providerUrl) {
        this.providerUrl = providerUrl;
        this.codec = Codec.getInstance(providerUrl.getOption(CODEC, CODEC_VAL_DEFAULT));
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public void setProviderUrl(Url providerUrl) {
        this.providerUrl = providerUrl;
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }

    @Override
    public Collection<Channel> getChannels() {
        throw new RpcFrameworkException(this.getClass().getName() + " getChannels() method unsupport " + providerUrl);
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        throw new RpcFrameworkException(this.getClass().getName() + " getChannel(InetSocketAddress) method unsupport " + providerUrl);
    }
}
