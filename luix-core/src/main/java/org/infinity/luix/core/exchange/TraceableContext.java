package org.infinity.luix.core.exchange;


import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TraceableContext implements Serializable {
    private static final long                serialVersionUID = 5065115597463921555L;
    protected            AtomicLong          receiveTime      = new AtomicLong();
    protected            AtomicLong          sendTime         = new AtomicLong();
    protected            Map<String, String> traceInfoMap     = new ConcurrentHashMap<>();

    public long getReceiveTime() {
        return receiveTime.get();
    }

    public void setReceiveTime(long receiveTime) {
        this.receiveTime.compareAndSet(0, receiveTime);
    }

    public long getSendTime() {
        return sendTime.get();
    }

    public void setSendTime(long sendTime) {
        this.sendTime.compareAndSet(0, sendTime);
    }

    public void addTraceInfo(String key, String value) {
        traceInfoMap.put(key, value);
    }

    public String getTraceInfo(String key) {
        return traceInfoMap.get(key);
    }

    public Map<String, String> getTraceInfoMap() {
        return traceInfoMap;
    }

    @Override
    public String toString() {
        return "send: " + sendTime + ", receive: " + receiveTime + ", info: " + traceInfoMap;
    }
}
