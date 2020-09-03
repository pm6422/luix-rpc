package org.infinity.rpc.core.exchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public interface Traceable {
    AtomicLong          SENDING_TIME  = new AtomicLong();
    AtomicLong          RECEIVED_TIME = new AtomicLong();
    AtomicLong          ELAPSED_TIME  = new AtomicLong();
    Map<String, String> TRACES        = new ConcurrentHashMap<>();

    // Timestamp is a compatible format for the different systems
    void setSendingTime(long sendingTime);

    long getSendingTime();

    void setReceivedTime(long receivedTime);

    long getReceivedTime();

    void setElapsedTime(long elapsedTime);

    long getElapsedTime();

    Map<String, String> getTraces();

    void addTrace(String key, String value);

    String getTrace(String key);
}
