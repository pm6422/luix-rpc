package org.infinity.rpc.core.registry;


import org.infinity.rpc.utilities.collection.ArrayUtils;

public class Protocol {
    public static final String   ZOOKEEPER       = "zookeeper";
    public static final String[] VALID_PROTOCOLS = ArrayUtils.newArray(ZOOKEEPER);
}
