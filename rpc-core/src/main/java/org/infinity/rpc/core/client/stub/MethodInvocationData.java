package org.infinity.rpc.core.client.stub;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
public class MethodInvocationData implements Serializable {
    private static final long serialVersionUID = 6887529494015011116L;

    /**
     * Interface name
     */
    private String              interfaceName;
    /**
     * Method name
     */
    private String              methodName;
    /**
     * Method parameter list. e.g, {java.util.List,java.lang.Long}
     */
    private String[]            methodParamTypes;
    /**
     * Method arguments
     */
    private Object[]            args;
    /**
     * Consumer stub attributes map
     */
    private Map<String, String> attributes;
}
