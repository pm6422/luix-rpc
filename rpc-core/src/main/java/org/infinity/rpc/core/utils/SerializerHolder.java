package org.infinity.rpc.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.infinity.rpc.utilities.concurrent.ThreadSafe;
import org.infinity.rpc.utilities.serializer.Serializer;
import org.infinity.rpc.utilities.serviceloader.ServiceLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ThreadSafe
public abstract class SerializerHolder {
    private static final Map<Integer, Serializer> SERIALIZER_CACHE = new ConcurrentHashMap<>();

    public static synchronized void init() {
        try {
            ServiceLoader.forClass(Serializer.class).loadAll().forEach(s -> SERIALIZER_CACHE.put(s.getSerializerId(), s));
        } catch (Exception e) {
            log.error("Failed to load serializer", e);
        }
    }

    public static synchronized Serializer getSerializerById(int id) {
        return SERIALIZER_CACHE.get(id);
    }
}
