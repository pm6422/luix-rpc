package org.infinity.rpc.core.config;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApplicationExtConfig extends ApplicationConfig {

    public ApplicationExtConfig(ApplicationConfig applicationConfig) {
        setName(applicationConfig.getName());
        setDescription(applicationConfig.getDescription());
        setTeam(applicationConfig.getTeam());
        setOwnerMail(applicationConfig.getOwnerMail());
        setEnv(applicationConfig.getEnv());
    }

    /**
     * Infinity RPC jar version
     */
    private String  infinityRpcVersion;
    /**
     * Application start time
     */
    private String  startTime;
    /**
     *
     */
    private boolean active = false;
}
