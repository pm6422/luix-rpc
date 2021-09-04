package org.infinity.luix.core.codec;

import org.infinity.luix.core.exchange.Channel;
import org.infinity.luix.core.exchange.Exchangable;
import org.infinity.luix.utilities.serviceloader.ServiceLoader;
import org.infinity.luix.utilities.serviceloader.annotation.Spi;
import org.infinity.luix.utilities.serviceloader.annotation.SpiScope;

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
    byte[] encode(Channel channel, Exchangable inputObject) throws IOException;

    /**
     * Decode the input byte array to origin object
     *
     * @param channel  channel
     * @param remoteIp 在服务器端decode request时能获取到客户端的ip
     * @param data     data
     * @return output object
     * @throws IOException IOException if IOException thrown
     */
    Object decode(Channel channel, String remoteIp, byte[] data) throws IOException, ClassNotFoundException;

    /**
     * Get instance associated with the specified name
     *
     * @param name specified name
     * @return instance
     */
    static Codec getInstance(String name) {
        return ServiceLoader.forClass(Codec.class).load(name);
    }

}