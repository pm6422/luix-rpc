package org.infinity.rpc.core.server.stub;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class MethodMeta implements Serializable {
    private static final long         serialVersionUID = 934245299068304702L;
    /**
     * Method name
     */
    private              String       methodName;
    /**
     * Method parameter list. e.g, [java.util.List,java.lang.Long]
     */
    private              List<String> methodParamTypes;
    /**
     * Method signature. e.g, invoke(java.util.List,java.lang.Long)
     */
    private              String       methodSignature;
    /**
     * Return type. e.g, java.lang.Long
     */
    private              String       returnType;

}
