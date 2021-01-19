package org.infinity.rpc.core.exchange.server.messagehandler;

import org.infinity.rpc.core.exchange.transport.Channel;

public interface MessageHandler {

    Object handle(Channel channel, Object message);

}