package com.luixtech.rpc.core.utils;

import com.luixtech.utilities.serializer.Serializer;
import com.luixtech.utilities.serviceloader.ServiceLoader;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;
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
