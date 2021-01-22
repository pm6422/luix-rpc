package org.infinity.rpc.core.registry;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class App {
    /**
     * Application ID
     */
    private String id;
    /**
     * Application name
     */
    private String name;
    /**
     * Application description
     */
    private String description;
    /**
     * Responsible team
     */
    private String team;
    /**
     * Application owner
     */
    private String owner;
    /**
     * Environment variable, e.g. dev, test or prod
     */
    private String env;
    /**
     * Infinity RPC jar version
     */
    private String infinityRpcVersion;
    /**
     * Latest registered time
     */
    private String latestRegisteredTime;
}
