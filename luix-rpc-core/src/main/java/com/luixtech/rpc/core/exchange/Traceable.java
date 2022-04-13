package com.luixtech.rpc.core.exchange;

import java.util.Map;

public interface Traceable {
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
