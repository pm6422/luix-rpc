package org.infinity.rpc.core.codec.impl;

import org.infinity.rpc.core.codec.AbstractCodec;
import org.infinity.rpc.core.exchange.Channel;
import org.infinity.rpc.core.exchange.Exchangable;
import org.infinity.rpc.utilities.serviceloader.annotation.SpiName;

import java.io.IOException;

import static org.infinity.rpc.core.constant.ProtocolConstants.CODEC_VAL_V2;

@SpiName(CODEC_VAL_V2)
public class CodecV2 extends AbstractCodec {
    @Override
    public byte[] encode(Channel channel, Exchangable inputObject) throws IOException {
        return new byte[0];
    }

    @Override
    public Object decode(Channel channel, String remoteIp, byte[] data) throws IOException {
        return null;
    }
}
