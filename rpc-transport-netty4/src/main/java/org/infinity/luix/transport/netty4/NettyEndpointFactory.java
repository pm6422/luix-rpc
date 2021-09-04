package org.infinity.luix.transport.netty4;

import org.infinity.luix.core.server.messagehandler.MessageHandler;
import org.infinity.luix.core.exchange.client.Client;
import org.infinity.luix.core.exchange.endpoint.AbstractEndpointFactory;
import org.infinity.luix.core.exchange.server.Server;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.transport.netty4.client.NettyClient;
import org.infinity.luix.transport.netty4.server.NettyServer;
import org.infinity.luix.utilities.serviceloader.annotation.SpiName;

import static org.infinity.luix.core.constant.ProtocolConstants.ENDPOINT_FACTORY_VAL_NETTY;

@SpiName(ENDPOINT_FACTORY_VAL_NETTY)
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
