package org.infinity.rpc.transport.netty4;

import org.infinity.rpc.core.server.messagehandler.MessageHandler;
import org.infinity.rpc.core.exchange.client.Client;
import org.infinity.rpc.core.exchange.endpoint.AbstractEndpointFactory;
import org.infinity.rpc.core.exchange.server.Server;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.transport.netty4.client.NettyClient;
import org.infinity.rpc.transport.netty4.server.NettyServer;
import org.infinity.rpc.utilities.spi.annotation.SpiName;

@SpiName("netty")
public class NettyEndpointFactory extends AbstractEndpointFactory {
    @Override
    protected Server innerCreateServer(Url providerUrl, MessageHandler messageHandler) {
        return new NettyServer(providerUrl, messageHandler);
    }

    @Override
    protected Client innerCreateClient(Url providerUrl) {
        return new NettyClient(providerUrl);
    }
}
