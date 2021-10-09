package org.infinity.luix.core.exchange.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.core.codec.Codec;
import org.infinity.luix.core.constant.ProtocolConstants;
import org.infinity.luix.core.exception.impl.RpcFrameworkException;
import org.infinity.luix.core.exchange.constants.ChannelState;
import org.infinity.luix.core.url.Url;

import java.net.InetSocketAddress;

@Slf4j
@Data
public abstract class AbstractClient implements Client {
    /**
     * Set default state with ChannelState.UNINITIALIZED
     */
    protected volatile ChannelState      state = ChannelState.UNINITIALIZED;
    protected          Url               providerUrl;
    protected          Codec             codec;
    protected          InetSocketAddress localAddress;
    protected          InetSocketAddress remoteAddress;

    public AbstractClient(Url providerUrl) {
        this.providerUrl = providerUrl;
        String codecName = providerUrl.getOption(ProtocolConstants.CODEC, ProtocolConstants.CODEC_VAL_DEFAULT);
        this.codec = Codec.getInstance(codecName);
        if (codec == null) {
            throw new RpcFrameworkException("Illegal codec name [" + codecName + "]!");
        }
        log.info("Initializing client by {} for url [{}]", codec.getClass().getSimpleName(), providerUrl);
    }
}
