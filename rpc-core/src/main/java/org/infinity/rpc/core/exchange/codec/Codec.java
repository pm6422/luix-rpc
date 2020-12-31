package org.infinity.rpc.core.exchange.codec;

import org.infinity.rpc.core.exchange.transmission.Channel;
import org.infinity.rpc.utilities.spi.annotation.Spi;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;

import java.io.IOException;

@Spi(scope = SpiScope.PROTOTYPE)
public interface Codec {

    /**
     * Encode the input object to byte array
     *
     * @param channel     channel
     * @param inputObject input object
     * @return encoded request data
     * @throws IOException if IOException thrown
     */
    byte[] encode(Channel channel, Object inputObject) throws IOException;

    /**
     * Decode the input byte array to origin object
     *
     * @param channel  channel
     * @param remoteIp 在服务器端decode request时能获取到客户端的ip
     * @param data   data
     * @return output object
     * @throws IOException IOException if IOException thrown
     */
    Object decode(Channel channel, String remoteIp, byte[] data) throws IOException;

}