package org.infinity.rpc.core.exchange.transmission.codec;

import org.infinity.rpc.core.exchange.transmission.Channel;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import java.io.IOException;

@Spi(scope = SpiScope.PROTOTYPE)
public interface Codec {

    /**
     * @param channel
     * @param message
     * @return
     * @throws IOException
     */
    byte[] encode(Channel channel, Object message) throws IOException;

    /**
     * @param channel
     * @param remoteIp 用来在server端decode request时能获取到client的ip。
     * @param buffer
     * @return
     * @throws IOException
     */
    Object decode(Channel channel, String remoteIp, byte[] buffer) throws IOException;

}