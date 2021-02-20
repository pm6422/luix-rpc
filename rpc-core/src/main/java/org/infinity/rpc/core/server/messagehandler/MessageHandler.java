package org.infinity.rpc.core.server.messagehandler;

import org.infinity.rpc.core.exchange.Channel;

public interface MessageHandler {

    Object handle(Channel channel, Object message);

}