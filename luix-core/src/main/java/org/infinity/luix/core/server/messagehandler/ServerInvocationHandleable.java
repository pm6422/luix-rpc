package org.infinity.luix.core.server.messagehandler;

import org.infinity.luix.core.exchange.Channel;

public interface ServerInvocationHandleable {

    Object handle(Channel channel, Object message);

}