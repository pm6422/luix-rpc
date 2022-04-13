package com.luixtech.luixrpc.transport.netty4;

import com.luixtech.luixrpc.core.server.handler.InvocationHandleable;
import com.luixtech.luixrpc.core.exchange.client.Client;
import com.luixtech.luixrpc.core.exchange.endpoint.AbstractNetworkTransmissionFactory;
import com.luixtech.luixrpc.core.exchange.server.Server;
import com.luixtech.luixrpc.core.url.Url;
import com.luixtech.luixrpc.transport.netty4.client.NettyClient;
import com.luixtech.luixrpc.transport.netty4.server.NettyServer;
import com.luixtech.luixrpc.utilities.serviceloader.annotation.SpiName;

import static com.luixtech.luixrpc.core.constant.ProtocolConstants.NETWORK_TRANSMISSION_VAL_NETTY;

@SpiName(NETWORK_TRANSMISSION_VAL_NETTY)
public class NettyNetworkTransmissionFactory extends AbstractNetworkTransmissionFactory {

    @Override
    protected Client doCreateClient(Url providerUrl) {
        return new NettyClient(providerUrl);
    }

    @Override
    protected Server doCreateServer(Url providerUrl, InvocationHandleable handler) {
        return new NettyServer(providerUrl, handler);
    }
}
