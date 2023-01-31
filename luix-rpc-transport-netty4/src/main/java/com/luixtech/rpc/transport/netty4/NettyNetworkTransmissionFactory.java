package com.luixtech.rpc.transport.netty4;

import com.luixtech.rpc.core.exchange.client.Client;
import com.luixtech.rpc.core.exchange.endpoint.AbstractNetworkTransmissionFactory;
import com.luixtech.rpc.core.exchange.server.Server;
import com.luixtech.rpc.core.server.handler.InvocationHandleable;
import com.luixtech.rpc.core.url.Url;
import com.luixtech.rpc.transport.netty4.client.NettyClient;
import com.luixtech.rpc.transport.netty4.server.NettyServer;
import com.luixtech.utilities.serviceloader.annotation.SpiName;

import static com.luixtech.rpc.core.constant.ProtocolConstants.NETWORK_TRANSMISSION_VAL_NETTY;

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
