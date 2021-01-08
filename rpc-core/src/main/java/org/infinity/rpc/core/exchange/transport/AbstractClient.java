package org.infinity.rpc.core.exchange.transport;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.exception.RpcFrameworkException;
import org.infinity.rpc.core.exchange.codec.Codec;
import org.infinity.rpc.core.exchange.transport.constants.ChannelState;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.spi.ServiceLoader;

import java.net.InetSocketAddress;

@Slf4j
public abstract class AbstractClient implements Client {
    /**
     * Set default state with ChannelState.UNINITIALIZED
     */
    protected volatile ChannelState      state = ChannelState.UNINITIALIZED;
    protected          Url               url;
    protected          Codec             codec;
    protected          InetSocketAddress localAddress;
    protected          InetSocketAddress remoteAddress;

    public AbstractClient(Url url) {
        this.url = url;
        String codecName = url.getParameter(Url.PARAM_CODEC, Url.PARAM_CODEC_DEFAULT_VALUE);
        this.codec = ServiceLoader.forClass(Codec.class).load(codecName);
        if (codec == null) {
            throw new RpcFrameworkException("Codec [" + codecName + "] must not be null!");
        }

        log.info("Initializing client with codec {} for url: {}", codec.getClass().getSimpleName(), url);
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }
}
