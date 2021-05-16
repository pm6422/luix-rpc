package org.infinity.rpc.core.serialization.impl.kryo.factory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import de.javakaffee.kryoserializers.*;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.InvocationHandler;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public abstract class AbstractKryoFactory {

    private static final Set<Class<?>>                CUSTOM_CLASSES           = new LinkedHashSet<>();
    private static final Map<Class<?>, Serializer<?>> CUSTOM_CLASS_SERIALIZERS = new LinkedHashMap<>();
    private volatile     boolean                      created                  = false;

    /**
     * Register the class with default serializer
     *
     * @param clazz class type
     */
    public void registerClass(Class<?> clazz) {
        Validate.validState(!created, "Can NOT register class after created kryo instance!");
        CUSTOM_CLASSES.add(clazz);
    }

    /**
     * Register the class with specified serializer
     *
     * @param clazz      class type
     * @param serializer serializer
     */
    public void registerClass(Class<?> clazz, Serializer<?> serializer) {
        Validate.validState(!created, "Can NOT register class serializer after created kryo instance!");
        CUSTOM_CLASS_SERIALIZERS.put(clazz, serializer);
    }

    public Kryo createInstance() {
        if (!created) {
            created = true;
        }

        Kryo kryo = new CompatibleKryo();
        kryo.setRegistrationRequired(false);
        kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());

        // Register some known classes for performance optimization
        kryo.register(Collections.singletonList("").getClass(), new ArraysAsListSerializer());
        kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
        kryo.register(InvocationHandler.class, new JdkProxySerializer());
        kryo.register(BigDecimal.class, new DefaultSerializers.BigDecimalSerializer());
        kryo.register(BigInteger.class, new DefaultSerializers.BigIntegerSerializer());
        kryo.register(Pattern.class, new RegexSerializer());
        kryo.register(BitSet.class, new BitSetSerializer());
        kryo.register(URI.class, new URISerializer());
        kryo.register(UUID.class, new UUIDSerializer());
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);
        SynchronizedCollectionsSerializer.registerSerializers(kryo);

        kryo.register(HashMap.class);
        kryo.register(ArrayList.class);
        kryo.register(LinkedList.class);
        kryo.register(HashSet.class);
        kryo.register(TreeSet.class);
        kryo.register(Hashtable.class);
        kryo.register(Date.class);
        kryo.register(Instant.class);
        kryo.register(LocalDate.class);
        kryo.register(LocalDateTime.class);
        kryo.register(Calendar.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(SimpleDateFormat.class);
        kryo.register(GregorianCalendar.class);
        kryo.register(Vector.class);
        kryo.register(BitSet.class);
        kryo.register(StringBuffer.class);
        kryo.register(StringBuilder.class);
        kryo.register(Object.class);
        kryo.register(Object[].class);
        kryo.register(String[].class);
        kryo.register(byte[].class);
        kryo.register(char[].class);
        kryo.register(int[].class);
        kryo.register(float[].class);
        kryo.register(double[].class);

        // Register user custom classes
        CUSTOM_CLASSES.forEach(kryo::register);

        // Register user custom class serializers
        CUSTOM_CLASS_SERIALIZERS.forEach(kryo::register);

        return kryo;
    }


    /**
     * Get kryo instance
     *
     * @return kryo instance
     */
    public abstract Kryo getKryo();

    /**
     * Release kryo instance
     *
     * @param kryo kryo instance
     */
    public abstract void releaseKryo(Kryo kryo);
}