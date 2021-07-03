package org.infinity.rpc.webcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Map;

/**
 * {
 * "interfaceName": "org.infinity.rpc.democommon.service.AuthorityService",
 * "methodName": "findAll",
 * "methodParamTypes": [],
 * "args": [],
 * "attributes": {
 * "retryCount": 2
 * }
 * }
 * <p>
 * or
 * <p>
 * {
 * "interfaceName": "org.infinity.rpc.democommon.service.AuthorityService",
 * "methodName": "save",
 * "methodParamTypes": [
 * "org.infinity.rpc.democommon.domain.Authority"
 * ],
 * "args": [
 * {
 * "name": "ROLE_TEST",
 * "enabled": true
 * }
 * ],
 * "attributes": {}
 * }
 */
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
     */
    private              String              interfaceName;
    /**
     * Provider url
     */
    @NotEmpty
    private              String              providerUrl;
    /**
     * Method name. e.g, save
     */
    @NotEmpty
    private              String              methodName;
    /**
     * Method parameter list. e.g, ["org.infinity.rpc.democommon.domain.Authority"]
     */
    private              String[]            methodParamTypes;
    /**
     * Method arguments, e.g, [{"name": "ROLE_TEST","enabled": true}]
     */
    private              Object[]            args;
    /**
     * Consumer stub attributes map
     */
    private              Map<String, String> attributes;
}