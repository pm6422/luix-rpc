package org.infinity.luix.core.exchange.constants;

public enum ChannelState {
    /**
     * Created state
     */
    CREATED(0),
    /**
     * Initialized state
     */
    INITIALIZED(1),
    /**
     * Active state
     */
    ACTIVE(2),
    /**
     * Inactive state
     */
    INACTIVE(3),
    /**
     * Closed state
     */
    CLOSED(4);

    public final int value;

    ChannelState(int value) {
        this.value = value;
    }

    public boolean isCreated() {
        return this == CREATED;
    }

    public boolean isInitialized() {
        return this == INITIALIZED;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isInactive() {
        return this == INACTIVE;
    }

    public boolean isClosed() {
        return this == CLOSED;
    }
}
