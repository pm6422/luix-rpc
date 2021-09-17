package org.infinity.luix.demoserver.task.polling.queue;

import java.util.HashMap;
import java.util.Map;

public class MessageQueue {
    private static final Map<String, Message> MAP = new HashMap<>(16);

    public static void put(Message msg) {
        MAP.put(msg.getId(), msg);
    }

    public static Message get(String id) {
        return MAP.get(id);
    }
}
