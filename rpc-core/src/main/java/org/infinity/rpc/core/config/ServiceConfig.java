package org.infinity.rpc.core.config;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

import static org.infinity.rpc.core.constant.ServiceConstants.*;

@Data
public abstract class ServiceConfig implements Configurable {
    /**
     * Group
     */
    @NotEmpty
    private String group          = GROUP_VAL_DEFAULT;
    /**
     * Version
     */
    @NotEmpty
    private String version        = VERSION_VAL_DEFAULT;
    /**
     * Check health factory
     */
    @NotEmpty
    private String healthChecker  = HEALTH_CHECKER_VAL_DEFAULT;
    /**
     * Timeout in milliseconds for handling request between client and server sides
     */
    private int    requestTimeout = REQUEST_TIMEOUT_VAL_DEFAULT;
    /**
     * Max retry count after calling failure
     */
    private int    maxRetries     = MAX_RETRIES_VAL_DEFAULT;
    /**
     * Max request/response message data payload size in bytes
     * NOT support configuration per consumer/provider level
     */
    private int    maxPayload     = MAX_PAYLOAD_VAL_DEFAULT;
}
