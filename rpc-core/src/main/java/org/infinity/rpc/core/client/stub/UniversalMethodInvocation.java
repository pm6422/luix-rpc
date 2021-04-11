package org.infinity.rpc.core.client.stub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * {
 *   "interfaceName": "org.infinity.rpc.democommon.service.AuthorityService",
 *   "methodName": "findAll",
 *   "methodParamTypes": [],
 *   "args": [],
 *   "attributes": {
 *     "maxRetries": 2
 *   }
 * }
 *
 * or
 *
 * {
 *   "interfaceName": "org.infinity.rpc.democommon.service.AuthorityService",
 *   "methodName": "save",
 *   "methodParamTypes": [
 *     "org.infinity.rpc.democommon.domain.Authority"
 *   ],
 *   "args": [
 *     {
 *       "name": "ROLE_TEST",
 *       "enabled": true
 *     }
 *   ],
 *   "attributes": {}
 * }
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UniversalMethodInvocation extends MethodInvocation implements Serializable {
    private static final long                serialVersionUID = 2948876928729993111L;
    /**
     * Interface name
     */
    private              String              interfaceName;
    /**
     * Consumer stub attributes map
     */
    private              Map<String, String> attributes;
}
