package com.luixtech.luixrpc.core.server.stub;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class MethodMeta implements Serializable {
    private static final long                serialVersionUID  = 934245299068304702L;
    private static final String              CATEGORY_NUMBER   = "number";
    private static final String              CATEGORY_STRING   = "string";
    private static final String              CATEGORY_OBJECT   = "object";
    private static final Map<String, String> TYPE_CATEGORY_MAP = new HashMap<>();
    /**
     * Method name
     */
    private              String              methodName;
    /**
     * Method parameter type list. e.g, [java.util.List,java.lang.Long]
     */
    private              List<String>        methodParamTypes;
    /**
     * Method parameter type category list. e.g, [string,number,object]
     */
    private              List<String>        methodParamCategories;
    /**
     * Method signature. e.g, invoke(java.util.List,java.lang.Long)
     */
    private              String              methodSignature;
    /**
     * Return type. e.g, java.lang.Long
     */
    private              String              returnType;

    static {
        TYPE_CATEGORY_MAP.put(Short.class.getName(), CATEGORY_NUMBER);
        TYPE_CATEGORY_MAP.put("short", CATEGORY_NUMBER);

        TYPE_CATEGORY_MAP.put(Integer.class.getName(), CATEGORY_NUMBER);
        TYPE_CATEGORY_MAP.put("int", CATEGORY_NUMBER);

        TYPE_CATEGORY_MAP.put(Long.class.getName(), CATEGORY_NUMBER);
        TYPE_CATEGORY_MAP.put("long", CATEGORY_NUMBER);

        TYPE_CATEGORY_MAP.put(Float.class.getName(), CATEGORY_NUMBER);
        TYPE_CATEGORY_MAP.put("float", CATEGORY_NUMBER);

        TYPE_CATEGORY_MAP.put(Double.class.getName(), CATEGORY_NUMBER);
        TYPE_CATEGORY_MAP.put("double", CATEGORY_NUMBER);

        TYPE_CATEGORY_MAP.put(Byte.class.getName(), CATEGORY_NUMBER);
        TYPE_CATEGORY_MAP.put("byte", CATEGORY_NUMBER);

        TYPE_CATEGORY_MAP.put(Boolean.class.getName(), CATEGORY_STRING);
        TYPE_CATEGORY_MAP.put("boolean", CATEGORY_STRING);

        TYPE_CATEGORY_MAP.put(String.class.getName(), CATEGORY_STRING);

        TYPE_CATEGORY_MAP.put(Character.class.getName(), CATEGORY_STRING);
    }

    public MethodMeta(String methodName, List<String> methodParamTypes, String methodSignature, String returnType) {
        this.methodName = methodName;
        this.methodParamTypes = methodParamTypes;
        this.methodSignature = methodSignature;
        this.returnType = returnType;

        if (CollectionUtils.isNotEmpty(methodParamTypes)) {
            methodParamCategories = new ArrayList<>(methodParamTypes.size());
            methodParamTypes.forEach(type -> {
                String category = TYPE_CATEGORY_MAP.get(type);
                if (category != null) {
                    methodParamCategories.add(category);
                } else {
                    methodParamCategories.add(CATEGORY_OBJECT);
                }
            });
        }
    }
}
