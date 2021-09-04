package org.infinity.luix.utilities.id;


import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.utilities.concurrent.ThreadSafe;
import org.infinity.luix.utilities.network.AddressUtils;

import static org.apache.commons.lang3.StringUtils.substring;

@ThreadSafe
public abstract class IdGenerator {
    private static final ShortIdGenerator     SHORT_ID_GENERATOR      = new ShortIdGenerator();
    private static final SnowFlakeIdGenerator SNOW_FLAKE_ID_GENERATOR = new SnowFlakeIdGenerator(1L, false, false);
    private static final String               IP_SUFFIX               = substring(AddressUtils.getLocalIp().replaceAll(".", StringUtils.EMPTY), -3);

    /**
     * Generate a thread-safe digit format ID
     *
     * @return 19 bits length，e.g：1672888135850179037
     */
    public static long generateTimestampId() {
        return TimestampIdGenerator.nextId();
    }

    /**
     * Generate a thread-safe digit format ID string with unique value under multiple hosts cluster environment
     * The first three bits are the IP suffix
     *
     * @return 22 bits length，e.g：1281672888135850179037
     */
    public static String generateUniqueId() {
        return IP_SUFFIX + generateTimestampId();
    }

    /**
     * Generate a thread-safe digit format ID, it can be used in a low concurrency environment
     *
     * @return 12 bits length，e.g：306554419571
     */
    public static long generateShortId() {
        return SHORT_ID_GENERATOR.nextId();
    }

}
