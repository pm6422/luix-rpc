package org.infinity.rpc.core.config;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ApplicationExtConfig extends ApplicationConfig implements Serializable {

    private static final long   serialVersionUID = -7916757272373849145L;
    /**
     * Infinity RPC jar version
     */
    private              String infinityRpcVersion;

    public ApplicationExtConfig(ApplicationConfig applicationConfig) {
        setName(applicationConfig.getName());
        setDescription(applicationConfig.getDescription());
        setTeam(applicationConfig.getTeam());
        setOwnerMail(applicationConfig.getOwnerMail());
        setEnv(applicationConfig.getEnv());
    }
}
