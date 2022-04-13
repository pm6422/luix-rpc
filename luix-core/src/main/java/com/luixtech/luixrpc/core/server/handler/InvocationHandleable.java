package com.luixtech.luixrpc.core.server.handler;

import com.luixtech.luixrpc.core.exchange.Channel;

public interface InvocationHandleable {

    Object handle(Channel channel, Object message);

}