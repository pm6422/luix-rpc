package org.infinity.rpc.core.exchange.transport;

public interface MessageHandler {

    Object handle(Channel channel, Object message);

}