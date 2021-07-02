package org.infinity.rpc.webcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

/**
 * {
 * "methodName": "findAll",
 * "methodParamTypes": [],
 * "args": []
 * }
 * <p>
 * or
 * <p>
 * {
 * "methodName": "save",
 * "methodParamTypes": [
 * "org.infinity.rpc.democommon.domain.Authority"
 * ],
 * "args": [
 * {
 * "name": "ROLE_TEST",
 * "enabled": true
 * }
 * ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodInvocation implements Serializable {
    private static final long serialVersionUID = 6887529494015011116L;

    /**
     * Registry identity
     */
    @NotEmpty
    private String   registryIdentity = "zookeeper://localhost:2181/registry";
    /**
     * Provider url
     */
    @NotEmpty
    private String   providerUrl;
    /**
     * Method name. e.g, save
     */
    @NotEmpty
    private String   methodName;
    /**
     * Method parameter list. e.g, ["org.infinity.rpc.democommon.domain.Authority"]
     */
    private String[] methodParamTypes;
    /**
     * Method arguments, e.g, [{"name": "ROLE_TEST","enabled": true}]
     */
    private Object[] args;
}