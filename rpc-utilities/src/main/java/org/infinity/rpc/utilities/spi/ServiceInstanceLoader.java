package org.infinity.rpc.utilities.spi;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.infinity.rpc.utilities.spi.annotation.SpiScope;
import org.infinity.rpc.utilities.spi.annotation.ServiceName;
import org.infinity.rpc.utilities.spi.annotation.Spi;

import javax.annotation.concurrent.ThreadSafe;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A utility used to load a specified implementation of a service interface.
 * It carries out similar functions as {@link ServiceLoader}
 * Service providers can be installed in an implementation of the Java platform in the form of
 * jar files placed into any of the usual extension directories. Providers can also be made available by adding them to the
 * application's class path or by some other platform-specific means.
 * <p>
 * Requirements:
 * _ The service provider interface must be an interface class, not a concrete or abstract class
 * _ The service provider implementation class must have a zero-argument constructor so that they can be instantiated during loading
 * - The service provider is identified by placing a configuration file in the resource directory META-INF/services/
 * - The configuration file must be encoded in UTF-8
 * - The configuration file's name is the fully-qualified name of service provider interface
 * - The configuration file's contents are the fully-qualified name of service provider implementation class
 *
 * @param <T>
 */
//todo: rename
@Slf4j
@ThreadSafe
public class ServiceInstanceLoader<T> {
    /**
     * Service directory prefix
     */
    private static final String                                SERVICE_DIR_PREFIX          = "META-INF/services/";
    /**
     * Charset of the service configuration file
     */
    public static final  Charset                               SERVICE_CONFIG_FILE_CHARSET = StandardCharsets.UTF_8;
    /**
     * Cache used to store service loader
     */
    private static final Map<String, ServiceInstanceLoader<?>> SERVICE_LOADERS_CACHE       = new ConcurrentHashMap<>();
    /**
     * The class loader used to locate, load and instantiate service
     */
    private final        ClassLoader                           classLoader;
    /**
     * The interface representing the service being loaded
     */
    private final        Class<T>                              serviceInterface;
    /**
     * The loaded service classes
     */
    private final        Map<String, Class<T>>                 serviceImplClasses;
    /**
     * The loaded service instances
     */
    private final        Map<String, T>                        serviceImplInstances        = new ConcurrentHashMap<>();

    /**
     * Prohibit instantiate an instance outside the class
     */
    private ServiceInstanceLoader(Class<T> serviceInterface) {
        this(Thread.currentThread().getContextClassLoader(), serviceInterface);
    }

    /**
     * Prohibit instantiate an instance outside the class
     *
     * @param classLoader      class loader
     * @param serviceInterface service interface
     */
    private ServiceInstanceLoader(ClassLoader classLoader, Class<T> serviceInterface) {
        this.classLoader = classLoader;
        this.serviceInterface = serviceInterface;
        serviceImplClasses = loadImplClasses();
    }

    /**
     * Load service instance based on service configuration file
     *
     * @return service instances map
     */
    private ConcurrentMap<String, Class<T>> loadImplClasses() {
        String serviceFileName = SERVICE_DIR_PREFIX.concat(serviceInterface.getName());
        List<String> serviceImplClassNames = new ArrayList<>();
        try {
            Enumeration<URL> urls;
            if (classLoader == null) {
                urls = ClassLoader.getSystemResources(serviceFileName);
            } else {
                urls = classLoader.getResources(serviceFileName);
            }

            if (urls == null || !urls.hasMoreElements()) {
                return new ConcurrentHashMap<>();
            }

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                parseUrl(serviceInterface, url, serviceImplClassNames);
            }
        } catch (Exception e) {
            throw new RuntimeException();
            //todo
//            throw new MotanFrameworkException(
//                    "ExtensionLoader loadExtensionClasses error, prefix: " + prefix + " type: " + type.getClass(), e);
        }

        return loadImplClass(serviceImplClassNames);
    }

    private void parseUrl(Class<T> type, URL url, List<String> serviceImplClassNames) throws ServiceConfigurationError {
        // try-with-resource statement can automatically close the stream after use
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), SERVICE_CONFIG_FILE_CHARSET))) {
            String line;
            int indexNumber = 0;

            while ((line = reader.readLine()) != null) {
                indexNumber++;
                parseLine(type, url, line, indexNumber, serviceImplClassNames);
            }
        } catch (Exception x) {
            failLog(type, "Error reading spi configuration file", x);
        }
    }

    private void parseLine(Class<T> type, URL url, String line, int lineNumber, List<String> serviceImplClassNames) throws ServiceConfigurationError {
        int ci = line.indexOf('#');

        if (ci >= 0) {
            line = line.substring(0, ci);
        }

        line = line.trim();

        if (line.length() <= 0) {
            return;
        }

        if ((line.indexOf(' ') >= 0) || (line.indexOf('\t') >= 0)) {
            failThrows(type, url, lineNumber, "Illegal spi configuration-file syntax");
        }

        int cp = line.codePointAt(0);
        if (!Character.isJavaIdentifierStart(cp)) {
            failThrows(type, url, lineNumber, "Illegal spi provider-class name: " + line);
        }

        for (int i = Character.charCount(cp); i < line.length(); i += Character.charCount(cp)) {
            cp = line.codePointAt(i);
            if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
                failThrows(type, url, lineNumber, "Illegal spi provider-class name: " + line);
            }
        }

        if (!serviceImplClassNames.contains(line)) {
            serviceImplClassNames.add(line);
            log.debug("Created the implementation [{}] of interface [{}]", line, type.getName().substring(type.getName().lastIndexOf(".") + 1));
        }
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, Class<T>> loadImplClass(List<String> implClassNames) {
        ConcurrentMap<String, Class<T>> map = new ConcurrentHashMap<>();
        for (String implClassName : implClassNames) {
            try {
                Class<T> clz;
                if (classLoader == null) {
                    clz = (Class<T>) Class.forName(implClassName);
                } else {
                    clz = (Class<T>) Class.forName(implClassName, true, classLoader);
                }

                checkServiceImplType(clz);
                String spiName = getSpiServiceName(clz);
                if (map.containsKey(spiName)) {
                    failThrows(clz, ":Error spiName already exist " + spiName);
                } else {
                    map.put(spiName, clz);
                }
            } catch (Exception e) {
                failLog(serviceInterface, "Error load spi class", e);
            }
        }
        return map;
    }

    /**
     * Manually add service implementation class to service loader
     *
     * @param clz class to add to service loader
     */
    public void addServiceImplClass(Class<T> clz) {
        if (clz == null) {
            return;
        }
        checkServiceImplType(clz);
        String spiName = getSpiServiceName(clz);
        synchronized (serviceImplClasses) {
            if (serviceImplClasses.containsKey(spiName)) {
                failThrows(clz, ":Error spiName already exist " + spiName);
            } else {
                serviceImplClasses.put(spiName, clz);
            }
        }
    }

    private void checkServiceImplType(Class<T> clz) {
        checkClassPublic(clz);
        checkConstructorPublic(clz);
        checkClassInherit(clz);
    }

    private void checkClassPublic(Class<T> clz) {
        if (!Modifier.isPublic(clz.getModifiers())) {
            failThrows(clz, "Error is not a public class");
        }
    }

    private void checkConstructorPublic(Class<T> clz) {
        Constructor<?>[] constructors = clz.getConstructors();

        if (constructors.length == 0) {
            failThrows(clz, "Error has no public no-args constructor");
        }

        for (Constructor<?> constructor : constructors) {
            if (Modifier.isPublic(constructor.getModifiers()) && constructor.getParameterTypes().length == 0) {
                return;
            }
        }
        failThrows(clz, "Error has no public no-args constructor");
    }

    private void checkClassInherit(Class<T> clz) {
        if (!serviceInterface.isAssignableFrom(clz)) {
            failThrows(clz, "Error is not instanceof " + serviceInterface.getName());
        }
    }

    private String getSpiServiceName(Class<?> clz) {
        ServiceName serviceName = clz.getAnnotation(ServiceName.class);
        return (serviceName != null && !"".equals(serviceName.value())) ? serviceName.value() : clz.getSimpleName();
    }

    /**
     * todo: rename to getLoader
     * Get the service loader by service interface type
     *
     * @param serviceInterface provider interface with @Spi annotation
     * @param <T>              service interface type
     * @return the singleton service loader instance
     */
    public static <T> ServiceInstanceLoader<T> getServiceLoader(Class<T> serviceInterface) {
        checkValidity(serviceInterface);
        return createServiceLoader(serviceInterface);
    }

    private static <T> void checkValidity(Class<T> serviceInterface) {
        if (serviceInterface == null) {
            failThrows(null, "Service interface must NOT be null!");
        }

        if (!serviceInterface.isInterface()) {
            failThrows(serviceInterface, "Service interface must be interface class!");
        }

        if (!serviceInterface.isAnnotationPresent(Spi.class)) {
            failThrows(serviceInterface, "Service interface must be specified @Spi annotation!");
        }
    }

    /**
     * Create a service loader or get it from cache if exists
     *
     * @param serviceInterface service interface
     * @param <T>              service interface type
     * @return service instance loader cache instance
     */
    @SuppressWarnings("unchecked")
    private static synchronized <T> ServiceInstanceLoader<T> createServiceLoader(Class<T> serviceInterface) {
        ServiceInstanceLoader<T> loader = (ServiceInstanceLoader<T>) SERVICE_LOADERS_CACHE.get(serviceInterface.getName());
        if (loader == null) {
            loader = new ServiceInstanceLoader<>(serviceInterface);
            SERVICE_LOADERS_CACHE.put(serviceInterface.getName(), loader);
        }
        return loader;
    }

    /**
     * Get service implementation class by name
     *
     * @param name service implementation service name
     * @return implementation service class
     */
    public Class<T> getServiceImplClass(String name) {
        return serviceImplClasses.get(name);
    }

    /**
     * Get service implementation instance by name
     *
     * @param name service implementation service name
     * @return implementation service instance
     */
    public T load(String name) {
        Validate.notEmpty(name, "Service name must NOT be empty!");

        try {
            Spi spi = serviceInterface.getAnnotation(Spi.class);
            if (spi.scope() == SpiScope.SINGLETON) {
                return getSingletonServiceImpl(name);
            } else {
                return getPrototypeServiceImpl(name);
            }
        } catch (Exception e) {
            failThrows(serviceInterface, "Error when getExtension " + name, e);
        }
        return null;
    }

    private T getSingletonServiceImpl(String name) throws InstantiationException, IllegalAccessException {
        T obj = serviceImplInstances.get(name);
        if (obj != null) {
            return obj;
        }

        Class<T> clz = serviceImplClasses.get(name);
        if (clz == null) {
            return null;
        }

        synchronized (serviceImplInstances) {
            obj = serviceImplInstances.get(name);
            if (obj != null) {
                return obj;
            }
            obj = clz.newInstance();
            serviceImplInstances.put(name, obj);
        }
        return obj;
    }

    private T getPrototypeServiceImpl(String name) throws IllegalAccessException, InstantiationException {
        Class<T> clz = serviceImplClasses.get(name);
        if (clz == null) {
            return null;
        }
        return clz.newInstance();
    }

    private static <T> void failLog(Class<T> type, String msg, Throwable cause) {
        log.error(type.getName() + ": " + msg, cause);
    }

    private static <T> void failThrows(Class<T> type, String msg, Throwable cause) {
        throw new RuntimeException();
        //todo
//        throw new MotanFrameworkException(type.getName() + ": " + msg, cause);
    }

    private static <T> void failThrows(Class<T> type, String msg) {
        throw new RuntimeException();
        //todo
//        throw new MotanFrameworkException(type.getName() + ": " + msg);
    }

    private static <T> void failThrows(Class<T> type, URL url, int line, String msg) throws ServiceConfigurationError {
        failThrows(type, url + ":" + line + ": " + msg);
    }
}
