/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.infinity.rpc.core.extension;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <pre>
 * 	扩展JDK SPI
 * </pre>
 */
@Slf4j
public class CachedExtensionLoaderHolder<T> {
    // SPI path prefix
    private static final String                                      SPI_DIR_PREFIX          = "META-INF/services/";
    private static final Map<String, CachedExtensionLoaderHolder<?>> EXTENSION_LOADERS_CACHE = new ConcurrentHashMap<String, CachedExtensionLoaderHolder<?>>();
    private              ConcurrentMap<String, T>                    singletonInstances      = new ConcurrentHashMap<String, T>();
    private              ConcurrentMap<String, Class<T>>             extensionClasses        = null;
    private              Class<T>                                    extensionInterface;
    private              ClassLoader                                 classLoader;

    /**
     * Prohibit instantiate an instance
     */
    private CachedExtensionLoaderHolder(Class<T> extensionInterface) {
        this(extensionInterface, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Prohibit instantiate an instance
     */
    private CachedExtensionLoaderHolder(Class<T> extensionInterface, ClassLoader classLoader) {
        this.extensionInterface = extensionInterface;
        this.classLoader = classLoader;
        extensionClasses = loadExtensionClasses(SPI_DIR_PREFIX);
    }

    private ConcurrentMap<String, Class<T>> loadExtensionClasses(String spiDirPrefix) {
        String extensionFileName = spiDirPrefix.concat(extensionInterface.getName());
        List<String> classNames = new ArrayList<String>();

        try {
            Enumeration<URL> urls;
            if (classLoader == null) {
                urls = ClassLoader.getSystemResources(extensionFileName);
            } else {
                urls = classLoader.getResources(extensionFileName);
            }

            if (urls == null || !urls.hasMoreElements()) {
                return new ConcurrentHashMap<String, Class<T>>();
            }

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                parseUrl(extensionInterface, url, classNames);
            }
        } catch (Exception e) {
            throw new RuntimeException();
            //todo
//            throw new MotanFrameworkException(
//                    "ExtensionLoader loadExtensionClasses error, prefix: " + prefix + " type: " + type.getClass(), e);
        }

        return loadClass(classNames);
    }

    private void parseUrl(Class<T> type, URL url, List<String> classNames) throws ServiceConfigurationError {
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = url.openStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            String line = null;
            int indexNumber = 0;

            while ((line = reader.readLine()) != null) {
                indexNumber++;
                parseLine(type, url, line, indexNumber, classNames);
            }
        } catch (Exception x) {
            failLog(type, "Error reading spi configuration file", x);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException y) {
                failLog(type, "Error closing spi configuration file", y);
            }
        }
    }

    private void parseLine(Class<T> type, URL url, String line, int lineNumber, List<String> classNames) throws IOException, ServiceConfigurationError {
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

        if (!classNames.contains(line)) {
            classNames.add(line);
            log.debug("[{}] implementation: [{}]", type.getName().substring(type.getName().lastIndexOf(".") + 1), line);
        }
    }

    private ConcurrentMap<String, Class<T>> loadClass(List<String> classNames) {
        ConcurrentMap<String, Class<T>> map = new ConcurrentHashMap<String, Class<T>>();
        for (String className : classNames) {
            try {
                Class<T> clz;
                if (classLoader == null) {
                    clz = (Class<T>) Class.forName(className);
                } else {
                    clz = (Class<T>) Class.forName(className, true, classLoader);
                }

                checkExtensionType(clz);
                String spiName = getSpiName(clz);
                if (map.containsKey(spiName)) {
                    failThrows(clz, ":Error spiName already exist " + spiName);
                } else {
                    map.put(spiName, clz);
                }
            } catch (Exception e) {
                failLog(extensionInterface, "Error load spi class", e);
            }
        }
        return map;
    }

    private void checkExtensionType(Class<T> clz) {
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

        if (constructors == null || constructors.length == 0) {
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
        if (!extensionInterface.isAssignableFrom(clz)) {
            failThrows(clz, "Error is not instanceof " + extensionInterface.getName());
        }
    }

    public String getSpiName(Class<?> clz) {
        SpiMeta spiMeta = clz.getAnnotation(SpiMeta.class);
        String name = (spiMeta != null && !"".equals(spiMeta.name())) ? spiMeta.name() : clz.getSimpleName();
        return name;
    }

    /**
     * Get the extension loader instance
     *
     * @param extensionInterface extension interface with @Spi annotation
     * @param <T>
     * @return
     */
    public static <T> CachedExtensionLoaderHolder<T> getExtensionLoader(Class<T> extensionInterface) {
        checkValidity(extensionInterface);
        CachedExtensionLoaderHolder<T> loader = createExtensionLoader(extensionInterface);
        return loader;
    }

    private static <T> void checkValidity(Class<T> extensionInterface) {
        if (extensionInterface == null) {
            failThrows(extensionInterface, "Extension type must NOT be null!");
        }

        if (!extensionInterface.isInterface()) {
            failThrows(extensionInterface, "Extension type must be interface!");
        }

        if (!isSpiType(extensionInterface)) {
            failThrows(extensionInterface, "Extension type must be specified @Spi annotation!");
        }
    }

    private static <T> boolean isSpiType(Class<T> clz) {
        return clz.isAnnotationPresent(Spi.class);
    }

    public static synchronized <T> CachedExtensionLoaderHolder<T> createExtensionLoader(Class<T> extensionInterface) {
        CachedExtensionLoaderHolder<T> loader = (CachedExtensionLoaderHolder<T>) EXTENSION_LOADERS_CACHE.get(extensionInterface.getName());
        if (loader == null) {
            loader = new CachedExtensionLoaderHolder<T>(extensionInterface);
            EXTENSION_LOADERS_CACHE.put(extensionInterface.getName(), loader);
        }
        return loader;
    }


//    public Class<T> getExtensionClass(String name) {
////        initialize();
//        return extensionClasses.get(name);
//    }

    public T getExtension(String name) {
//        initialize();
        Validate.notEmpty(name, "Extension name must NOT be empty!");

        try {
            Spi spi = extensionInterface.getAnnotation(Spi.class);
            if (spi.scope() == Scope.SINGLETON) {
                return getSingletonInstance(name);
            } else {
                Class<T> clz = extensionClasses.get(name);
                if (clz == null) {
                    return null;
                }
                return clz.newInstance();
            }
        } catch (Exception e) {
            failThrows(extensionInterface, "Error when getExtension " + name, e);
        }

        return null;
    }

    private T getSingletonInstance(String name) throws InstantiationException, IllegalAccessException {
        T obj = singletonInstances.get(name);
        if (obj != null) {
            return obj;
        }

        Class<T> clz = extensionClasses.get(name);
        if (clz == null) {
            return null;
        }

        synchronized (singletonInstances) {
            obj = singletonInstances.get(name);
            if (obj != null) {
                return obj;
            }
            obj = clz.newInstance();
            singletonInstances.put(name, obj);
        }

        return obj;
    }

    public void addExtensionClass(Class<T> clz) {
        if (clz == null) {
            return;
        }
//        initialize();
        checkExtensionType(clz);

        String spiName = getSpiName(clz);

        synchronized (extensionClasses) {
            if (extensionClasses.containsKey(spiName)) {
                failThrows(clz, ":Error spiName already exist " + spiName);
            } else {
                extensionClasses.put(spiName, clz);
            }
        }
    }

//    private void initialize() {
//        if (!initialized) {
//            loadExtensionClasses();
//        }
//    }


    /**
     * 有些地方需要spi的所有激活的instances，所以需要能返回一个列表的方法 注意：1 SpiMeta 中的active 为true； 2
     * 按照spiMeta中的sequence进行排序 FIXME： 是否需要对singleton来区分对待，后面再考虑 fishermen
     *
     * @return
     */
    public List<T> getExtensions(String key) {
//        initialize();

        if (extensionClasses.size() == 0) {
            return Collections.emptyList();
        }

        // 如果只有一个实现，直接返回
        List<T> exts = new ArrayList<T>(extensionClasses.size());

        // 多个实现，按优先级排序返回
        for (Map.Entry<String, Class<T>> entry : extensionClasses.entrySet()) {
            Activation activation = entry.getValue().getAnnotation(Activation.class);
            if (StringUtils.isBlank(key)) {
                exts.add(getExtension(entry.getKey()));
            } else if (activation != null && activation.key() != null) {
                for (String k : activation.key()) {
                    if (key.equals(k)) {
                        exts.add(getExtension(entry.getKey()));
                        break;
                    }
                }
            }
        }
        Collections.sort(exts, new ActivationComparator<T>());
        return exts;
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
