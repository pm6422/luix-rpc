package org.infinity.luix.webcenter.task.polling.queue;

import org.infinity.luix.webcenter.dto.StatisticsDTO;

import java.util.HashMap;
import java.util.Map;

public class StatisticsResultQueue {
    private static final Map<String, StatisticsDTO> MAP = new HashMap<>(16);

    public static void put(String id, StatisticsDTO msg) {
        MAP.put(id, msg);
    }

    public static StatisticsDTO get(String id) {
        return MAP.get(id);
    }
}
