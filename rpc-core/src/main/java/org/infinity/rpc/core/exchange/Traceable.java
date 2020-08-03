package org.infinity.rpc.core.exchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public interface Traceable<T> {
    AtomicLong          SENDING_TIME  = new AtomicLong();
    AtomicLong          RECEIVED_TIME = new AtomicLong();
    AtomicLong          ELAPSED_TIME  = new AtomicLong();
    Map<String, String> TRACES        = new ConcurrentHashMap<>();

    // The timestamp format is compatible with the different systems
    T sendingTime(long sendingTime);

    long getSendingTime();

    T receivedTime(long receivedTime);

    long getReceivedTime();

    T elapsedTime(long elapsedTime);

    long getElapsedTime();

    Map<String, String> getTraces();

    T trace(String key, String value);

    String getTrace(String key);
}
