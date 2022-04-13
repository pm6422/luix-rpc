package com.luixtech.rpc.core.exchange.client;

import com.luixtech.rpc.core.codec.Codec;
import com.luixtech.rpc.core.constant.ProtocolConstants;
import com.luixtech.rpc.core.exchange.constants.ChannelState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import com.luixtech.rpc.core.url.Url;

import java.net.InetSocketAddress;

@Slf4j
@Data
public abstract class AbstractClient implements Client {
    /**
     * Set default state with 'CREATED'
     */
    protected volatile ChannelState      state = ChannelState.CREATED;
    protected          Url               providerUrl;
    protected          Codec             codec;
    protected          InetSocketAddress localAddress;
    protected          InetSocketAddress remoteAddress;

    public AbstractClient(Url providerUrl) {
        this.providerUrl = providerUrl;
        String codecName = providerUrl.getOption(ProtocolConstants.CODEC, ProtocolConstants.CODEC_VAL_DEFAULT);
        this.codec = Codec.getInstance(codecName);
        log.info("Initializing network client of [{}] by {}", providerUrl, codec.getClass().getSimpleName());
    }
}
