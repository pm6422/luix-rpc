package org.infinity.rpc.transport.netty4;

import org.infinity.rpc.core.exchange.request.Requestable;
import org.infinity.rpc.core.exchange.response.Responseable;
import org.infinity.rpc.core.exchange.transport.AbstractSharedPoolClient;
import org.infinity.rpc.core.exchange.transport.Channel;
import org.infinity.rpc.core.exchange.transport.SharedObjectFactory;
import org.infinity.rpc.core.exchange.transport.exception.TransmissionException;
import org.infinity.rpc.core.url.Url;

/**
 * toto: implements StatisticCallback
 */
public class NettyClient extends AbstractSharedPoolClient {
    public NettyClient(Url url) {
        super(url);
    }

    @Override
    protected SharedObjectFactory<Channel> createChannelFactory() {
        return null;
    }

    @Override
    public Responseable request(Requestable request) throws TransmissionException {
        return null;
    }

    @Override
    public boolean open() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public void close(int timeout) {

    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Url getUrl() {
        return null;
    }
}
