package org.infinity.rpc.spring.boot.config;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

import static org.infinity.rpc.core.constant.ServiceConstants.*;

@Validated
@Data
public class ServiceConfig {
    /**
     * Group
     */
    @NotEmpty
    private String  group              = GROUP_DEFAULT_VALUE;
    /**
     * Version
     */
    @NotEmpty
    private String  version            = VERSION_DEFAULT_VALUE;
    /**
     * Check health flag
     */
    private boolean checkHealth        = CHECK_HEALTH_DEFAULT_VALUE;
    /**
     * Check health factory
     */
    @NotEmpty
    private String  checkHealthFactory = CHECK_HEALTH_FACTORY_DEFAULT_VALUE;
    /**
     *
     */
    private int     requestTimeout     = REQUEST_TIMEOUT_DEFAULT_VALUE;
    /**
     *
     */
    private int     maxRetries         = MAX_RETRIES_DEFAULT_VALUE;
}
