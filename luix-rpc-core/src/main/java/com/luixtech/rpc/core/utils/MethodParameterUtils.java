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

package com.luixtech.rpc.core.utils;

import com.luixtech.rpc.core.client.request.Requestable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A utility used to handle method parameters
 */
public class MethodParameterUtils {
    public static final  String                PARAM_TYPE_STR_DELIMITER        = ",";
    public static final  String                VOID                            = "void";
    private static final String                ARRAY_TYPE_SUFFIX               = "[]";
    private static final Class<?>[]            EMPTY_CLASS_ARRAY               = new Class<?>[0];
    private static final String[]              PRIMITIVE_TYPES                 = new String[]{
            "boolean", "byte", "char", "double", "float", "int", "long", "short", "void"};
    private static final Class<?>[]            PRIMITIVE_CLASSES               = new Class[]{
            boolean.class, byte.class, char.class, double.class, float.class, int.class, long.class, short.class, Void.TYPE};
    private static final int                   PRIMITIVE_CLASS_NAME_MAX_LENGTH = 7;
    private static final int                   MAX_NEST_DEPTH                  = 2;
    private static final Map<String, Class<?>> NAME_TO_CLASS_MAP               = new ConcurrentHashMap<>();
    private static final Map<Class<?>, String> CLASS_TO_NAME_MAP               = new ConcurrentHashMap<>();

    /**
     * Get the method parameter type name list string which is separated by comma.
     * e.g, java.util.List,java.lang.Long
     *
     * @param method method
     * @return method parameter class name list string
     */
    public static String getMethodParameters(Method method) {
        if (ArrayUtils.isEmpty(method.getParameterTypes())) {
            return VOID;
        }
        return Arrays.stream(method.getParameterTypes())
                .map(MethodParameterUtils::getClassName)
                .collect(Collectors.joining(PARAM_TYPE_STR_DELIMITER));
    }

    /**
     * Get the method name with its parameter class name list string.
     * e.g, invoke(java.util.List,java.lang.Long)
     *
     * @param method method
     * @return method name with parameter class name list string
     */
    public static String getMethodSignature(Method method) {
        return getMethodSignature(method.getName(), getMethodParameters(method));
    }

    /**
     * Get the method name with its class name and parameter class name list string.
     * e.g, com.luixtech.rpc.democommon.service.AdminMenuService.getMenus(java.util.List,java.lang.Long)
     *
     * @param request RPC request
     * @return method name with parameter class name list string
     */
    public static String getFullMethodSignature(Requestable request) {
        return request.getInterfaceName() + "." + request.getMethodName() + "("
                + request.getMethodParameters() + ")";
    }

    /**
     * Get the method name with its parameter class name list string.
     * e.g, invoke(java.util.List,java.lang.Long)
     *
     * @param methodName       method name
     * @param methodParameters method parameter class name list string
     * @return method name with parameter class name list string
     */
    public static String getMethodSignature(String methodName, String methodParameters) {
        if (StringUtils.isEmpty(methodParameters)) {
            return methodName + "(" + VOID + ")";
        } else {
            return methodName + "(" + methodParameters + ")";
        }
    }

    /**
     * Get class list from the class name list string which is separated by comma
     *
     * @param classNameList class name list string which is separated by comma
     * @return class list
     * @throws ClassNotFoundException if any ClassNotFoundException thrown
     */
    public static Class<?>[] forNames(String classNameList) throws ClassNotFoundException {
        if (StringUtils.isEmpty(classNameList) || VOID.equals(classNameList)) {
            return EMPTY_CLASS_ARRAY;
        }
        String[] classNames = classNameList.split(PARAM_TYPE_STR_DELIMITER);
        Class<?>[] classTypes = new Class<?>[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            classTypes[i] = forName(classNames[i]);
        }
        return classTypes;
    }

    /**
     * Returns the {@code Class} object associated with the class or
     * interface with the given class string name.
     *
     * @param className class name
     * @return class
     * @throws ClassNotFoundException if any ClassNotFoundException thrown
     */
    public static Class<?> forName(String className) throws ClassNotFoundException {
        if (StringUtils.isEmpty(className)) {
            return null;
        }

        Class<?> clz = NAME_TO_CLASS_MAP.get(className);
        if (clz != null) {
            return clz;
        }

        clz = doForName(className);
        NAME_TO_CLASS_MAP.putIfAbsent(className, clz);
        return clz;
    }

    private static Class<?> doForName(String className) throws ClassNotFoundException {
        if (!className.endsWith(ARRAY_TYPE_SUFFIX)) {
            // Class name is not array
            Class<?> clz = getPrimitiveTypeClass(className);
            if (clz != null) {
                return clz;
            }
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        }

        // Handle array type
        int dimensionSize = 0;
        while (className.endsWith(ARRAY_TYPE_SUFFIX)) {
            dimensionSize++;
            className = className.substring(0, className.length() - 2);
        }

        int[] dimensions = new int[dimensionSize];
        Class<?> clz = getPrimitiveTypeClass(className);
        if (clz == null) {
            clz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        }
        return Array.newInstance(clz, dimensions).getClass();
    }

    /**
     * Get primitive type class associated with the given class string name.
     *
     * @param className class name, e.g. int
     * @return primitive class, e.g. int.class
     */
    public static Class<?> getPrimitiveTypeClass(String className) {
        if (className.length() > PRIMITIVE_CLASS_NAME_MAX_LENGTH) {
            return null;
        }
        // Binary search
        int index = Arrays.binarySearch(PRIMITIVE_TYPES, className);
        if (index < 0) {
            return null;
        }
        return PRIMITIVE_CLASSES[index];
    }

    /**
     * Returns the class name associated with the class.
     *
     * @param clz class
     * @return class name
     */
    public static String getClassName(Class<?> clz) {
        if (clz == null) {
            return null;
        }

        String className = CLASS_TO_NAME_MAP.get(clz);
        if (className != null) {
            return className;
        }

        className = doGetClassName(clz);
        CLASS_TO_NAME_MAP.putIfAbsent(clz, className);
        return className;
    }

    private static String doGetClassName(Class<?> clz) {
        if (!clz.isArray()) {
            // The class is not array
            return clz.getName();
        }

        // Handle array type
        StringBuilder sb = new StringBuilder();
        while (clz.isArray()) {
            sb.append("[]");
            clz = clz.getComponentType();
        }
        return clz.getName() + sb;
    }

    /**
     * Get the method list with the public modifier
     *
     * <pre>
     * 1）Excluding constructor
     * 2）Excluding Object.class
     * 3）Including all the public methods derived from super class
     * </pre>
     *
     * @param clz class
     * @return method list
     */
    public static List<Method> getPublicMethod(Class<?> clz) {
        return Arrays
                .stream(clz.getMethods()).filter(method -> Modifier.isPublic(method.getModifiers())
                        && method.getDeclaringClass() != Object.class)
                .collect(Collectors.toList());
    }

    /**
     * Get the default value associated with the type
     *
     * @param type type class
     * @return default value
     */
    public static Object getEmptyObject(Class<?> type) {
        return getEmptyObject(type, new HashMap<>(1), 0);
    }

    private static Object getEmptyObject(Class<?> type, Map<Class<?>, Object> emptyInstances, int nestDepth) {
        if (nestDepth > MAX_NEST_DEPTH) {
            return null;
        }
        if (type == null) {
            return null;
        } else if (type == boolean.class || type == Boolean.class) {
            return false;
        } else if (type == char.class || type == Character.class) {
            return '\0';
        } else if (type == byte.class || type == Byte.class) {
            return (byte) 0;
        } else if (type == short.class || type == Short.class) {
            return (short) 0;
        } else if (type == int.class || type == Integer.class) {
            return 0;
        } else if (type == long.class || type == Long.class) {
            return 0L;
        } else if (type == float.class || type == Float.class) {
            return 0F;
        } else if (type == double.class || type == Double.class) {
            return 0D;
        } else if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        } else if (type.isAssignableFrom(ArrayList.class)) {
            return new ArrayList<>(0);
        } else if (type.isAssignableFrom(HashSet.class)) {
            return new HashSet<>(0);
        } else if (type.isAssignableFrom(HashMap.class)) {
            return new HashMap<>(0);
        } else if (String.class.equals(type)) {
            return "";
        } else if (!type.isInterface()) {
            try {
                Object value = emptyInstances.get(type);
                if (value == null) {
                    value = type.getDeclaredConstructor().newInstance();
                    emptyInstances.put(type, value);
                }
                Class<?> cls = value.getClass();
                while (cls != null && cls != Object.class) {
                    Field[] fields = cls.getDeclaredFields();
                    for (Field field : fields) {
                        Object property = getEmptyObject(field.getType(), emptyInstances, nestDepth + 1);
                        if (property != null) {
                            try {
                                if(!field.canAccess(value)){
                                    field.setAccessible(true);
                                }
                                field.set(value, property);
                            } catch (Throwable e) {
                                // Leave blank intentionally
                            }
                        }
                    }
                    cls = cls.getSuperclass();
                }
                return value;
            } catch (Throwable e) {
                return null;
            }
        } else {
            return null;
        }
    }

}
