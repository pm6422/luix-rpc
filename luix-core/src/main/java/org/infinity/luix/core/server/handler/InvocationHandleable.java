package org.infinity.luix.core.server.handler;

import org.infinity.luix.core.exchange.Channel;

public interface InvocationHandleable {

    Object handle(Channel channel, Object message);

}