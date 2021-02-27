package org.infinity.rpc.democlient.dto;

import lombok.Data;

import java.util.Map;

@Data
public class GenericCallDTO {
    private String              interfaceName;
    private String              methodName;
    private String[]            methodParamTypes;
    private Object[]            args;
    private Map<String, String> options;
}
