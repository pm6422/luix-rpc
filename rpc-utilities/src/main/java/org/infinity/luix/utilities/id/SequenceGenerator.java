package org.infinity.luix.utilities.id;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;

/**
 * Distributed Sequence Generator. Inspired by Twitter snowflake:
 * https://github.com/twitter/snowflake/tree/snowflake-2010
 * <p>
 * This class should be used as a Singleton. Make sure that you create and reuse a Single instance of SequenceGenerator
 * per node in your distributed system cluster.
 *
 * @see <a href=
 * "https://www.callicoder.com/distributed-unique-id-sequence-number-generator/">distributed-unique-id-sequence-number-generator</a>
 */
public class SequenceGenerator {
    private static final int NODE_ID_BITS  = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final int MAX_NODE_ID  = (int) (Math.pow(2, NODE_ID_BITS) - 1);
    private static final int MAX_SEQUENCE = (int) (Math.pow(2, SEQUENCE_BITS) - 1);

    /**
     * Custom Epoch (January 1, 2020 Midnight UTC = 2020-01-01T00:00:00Z)
     */
    private static final long CUSTOM_EPOCH = 1577836800000L;

    private final int nodeId;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence      = 0L;

    /**
     * Create SequenceGenerator with a nodeId
     *
     * @param nodeId node ID
     */
    SequenceGenerator(int nodeId) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException(String.format("NodeId must be between %d and %d", 0, MAX_NODE_ID));
        }
        this.nodeId = nodeId;
    }

    /**
     * Let SequenceGenerator generate a nodeId
     */
    SequenceGenerator() {
        this.nodeId = createNodeId();
    }

    public synchronized long nextId() {
        long currentTimestamp = timestamp();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Invalid System Clock!");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence Exhausted, wait till next millisecond.
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            // reset sequence to start with zero for the next millisecond
            sequence = 0;
        }

        lastTimestamp = currentTimestamp;

        long id = currentTimestamp << (NODE_ID_BITS + SEQUENCE_BITS);
        id |= (nodeId << SEQUENCE_BITS);
        id |= sequence;
        return id;
    }

    /**
     * Get current timestamp in milliseconds, adjust for the custom epoch.
     *
     * @return current timestamp
     */
    private static long timestamp() {
        return Instant.now().toEpochMilli() - CUSTOM_EPOCH;
    }

    /**
     * Block and wait till next millisecond
     *
     * @param currentTimestamp current timestamp
     * @return timestamp
     */
    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp) {
            currentTimestamp = timestamp();
        }
        return currentTimestamp;
    }

    private int createNodeId() {
        int nodeId;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                }
            }
            nodeId = sb.toString().hashCode();
        } catch (Exception ex) {
            nodeId = (new SecureRandom().nextInt());
        }
        nodeId = nodeId & MAX_NODE_ID;
        return nodeId;
    }
}