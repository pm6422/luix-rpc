package org.infinity.rpc.core.serialization.impl.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.core.utils.ReflectionUtils;

@Slf4j
public class CompatibleKryo extends Kryo {

    /**
     * Kryo requires every class to provide a zero argument constructor. For any class does not match this condition, kryo have two ways:
     * 1. Use JavaSerializer,
     * 2. Set 'kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));', StdInstantiatorStrategy can generate an instance bypassing the constructor.
     * <p>
     * In practice, it's not possible for users to register kryo Serializer for every customized class. So in most cases, customized classes with/without zero argument constructor will
     * default to the default serializer.
     * It is the responsibility of kryo to handle with every standard jdk classes, so we will just escape these classes.
     *
     * @param type type
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
                log.warn(type + " has no zero-arg constructor therefore this will affect the serialization performance!");
            }
            return new JavaSerializer();
        }
        return super.getDefaultSerializer(type);
    }
}