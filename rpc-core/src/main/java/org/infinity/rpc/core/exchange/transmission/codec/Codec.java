package org.infinity.rpc.core.exchange.transmission.codec;

import org.infinity.rpc.core.exchange.transmission.Channel;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.io.IOException;

@Spi(scope = SpiScope.PROTOTYPE)
public interface Codec {

    /**
     * Encode the message object to bytes array
     *
     * @param channel channel
     * @param type message object
     * @return encoded request data
     * @throws IOException if IOException thrown
     */
    byte[] encode(Channel channel, Object type) throws IOException;

    /**
     * Decode the byte array to origin object
     *
     * @param channel  channel
     * @param remoteIp 用来在server端decode request时能获取到client的ip
     * @param buffer   buffer
     * @return decoded response object
     * @throws IOException IOException if IOException thrown
     */
    Object decode(Channel channel, String remoteIp, byte[] buffer) throws IOException;

}