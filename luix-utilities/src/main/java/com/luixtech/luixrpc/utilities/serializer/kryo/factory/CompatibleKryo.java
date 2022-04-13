package com.luixtech.luixrpc.utilities.serializer.kryo.factory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import com.luixtech.luixrpc.utilities.reflection.ReflectionUtils;

@Slf4j
public class CompatibleKryo extends Kryo {

    /**
     * Kryo requires the class to provide a zero-argument constructor.
     * For any class does not match this condition, kryo have two ways:
     * 1. Use JavaSerializer
     * 2. Set 'kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));'
     * StdInstantiatorStrategy can generate an instance bypassing the constructor.
     *
     * @param type type class
     * @return serializer
     */
    @Override
    public Serializer<?> getDefaultSerializer(Class type) {
        Validate.isTrue(type != null, "Type must NOT be null!");

        if (!ReflectionUtils.isJdkClass(type)
                && !type.isArray()
                && !type.isEnum()
                && !ReflectionUtils.hasZeroArgConstructor(type)) {
            if (log.isWarnEnabled()) {
                log.warn(type + " has NO zero-argument constructor therefore it will reduce serialization performance. " +
                        "You can register the custom Serializer for the type to remove the alert.");
            }
            // Use Java serializer
            return new JavaSerializer();
        }
        // Use default serializer
        return super.getDefaultSerializer(type);
    }
}