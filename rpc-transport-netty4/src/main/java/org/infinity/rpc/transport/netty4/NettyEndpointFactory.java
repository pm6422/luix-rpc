package org.infinity.rpc.transport.netty4;

import org.infinity.rpc.core.exchange.transport.Client;
import org.infinity.rpc.core.exchange.transport.MessageHandler;
import org.infinity.rpc.core.exchange.transport.endpoint.AbstractEndpointFactory;
import org.infinity.rpc.core.exchange.transport.server.Server;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.transport.netty4.server.NettyServer;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;

@ServiceName("netty")
public class NettyEndpointFactory extends AbstractEndpointFactory {
    @Override
    protected Server innerCreateServer(Url url, MessageHandler messageHandler) {
        return new NettyServer(url, messageHandler);
    }

    @Override
    protected Client innerCreateClient(Url url) {
        return new NettyClient(url);
    }
}
