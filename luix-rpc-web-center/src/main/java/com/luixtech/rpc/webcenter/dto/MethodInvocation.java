package com.luixtech.rpc.webcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodInvocation implements Serializable {
    private static final long                serialVersionUID = 2948876928729993111L;
    /**
     * Registry identity
     */
    @NotEmpty
    private              String              registryIdentity;
    /**
     * Interface name
     * interfaceName or providerUrl must have value
     */
    private              String              interfaceName;
    /**
     * Provider url
     * interfaceName or providerUrl must have value
     */
    private              String              providerUrl;
    /**
     * Method name. e.g, save
     */
    @NotEmpty
    private              String              methodName;
    /**
     * Method parameter list. e.g, ["com.luixtech.rpc.democommon.domain.Authority"]
     */
    private              String[]            methodParamTypes;
    /**
     * Method arguments, e.g, [{"name": "ROLE_TEST","enabled": true}]
     */
    private              Object[]            args;
    /**
     * Consumer stub attributes map
     */
    private              Map<String, String> attributes = new HashMap<>();
}