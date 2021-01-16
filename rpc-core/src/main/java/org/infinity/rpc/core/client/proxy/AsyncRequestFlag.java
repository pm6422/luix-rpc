package org.infinity.rpc.core.client.proxy;

public enum AsyncRequestFlag {
    ASYNC("async"),
    SYNC("sync");

    private final String value;

    AsyncRequestFlag(String value) {
        this.value = value;
    }

}