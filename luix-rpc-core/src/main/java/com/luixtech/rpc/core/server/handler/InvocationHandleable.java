package com.luixtech.rpc.core.server.handler;

import com.luixtech.rpc.core.exchange.Channel;

public interface InvocationHandleable {

    Object handle(Channel channel, Object message);

}