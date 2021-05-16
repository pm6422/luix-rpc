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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public abstract class AbstractKryoFactory {

    private final        Set<Class<?>>                customClasses = new LinkedHashSet<>();
    private static final Map<Class<?>, Serializer<?>> REGISTRATIONS = new LinkedHashMap<>();
    private              boolean                      registrationRequired;
    private volatile     boolean                      created;

    /**
     * Only supposed to be called at startup time
     * <p>
     * later may consider adding support for custom serializer, custom id, etc
     */
    public void registerClass(Class<?> clazz) {
        Validate.validState(!created, "Can NOT register class after creating kryo instance!");
        customClasses.add(clazz);
    }

    /**
     * only supposed to be called at startup time
     *
     * @param clazz      object type
     * @param serializer object serializer
     */
    public void registerClass(Class<?> clazz, Serializer<?> serializer) {
        Validate.isTrue(clazz != null, "Class registered to kryo can NOT be null!");
        REGISTRATIONS.put(clazz, serializer);
    }

    public Kryo createInstance() {
        if (!created) {
            created = true;
        }

        Kryo kryo = new CompatibleKryo();
        kryo.setRegistrationRequired(registrationRequired);

        kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());

        // Register some common classes for performance optimization
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
        customClasses.forEach(kryo::register);

        // Register user custom classes and serializers
        REGISTRATIONS.forEach(kryo::register);

        return kryo;
    }

    public void setRegistrationRequired(boolean registrationRequired) {
        this.registrationRequired = registrationRequired;
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