package com.luixtech.luixrpc.core.config.impl;

import lombok.Data;
import com.luixtech.luixrpc.core.config.Configurable;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

@Data
public abstract class ServiceConfig implements Configurable {
    /**
     * One service interface may have multiple implementations(forms),
     * It used to distinguish between different implementations of service provider interface
     */
    @NotEmpty
    private String  form;
    /**
     * When the service changes, such as adding or deleting methods, and interface parameters change,
     * the provider and consumer application instances need to be upgraded.
     * In order to deploy in a production environment without affecting user use,
     * a gradual migration scheme is generally adopted.
     * First upgrade some provider application instances,
     * and then use the same version number to upgrade some consumer instances.
     * The old version of the consumer instance calls the old version of the provider instance.
     * Observe that there is no problem and repeat this process to complete the upgrade.
     */
    @NotEmpty
    private String  version;
    /**
     * Timeout in milliseconds for handling request between client and server sides
     */
    @Min(value = 0, message = "The [timeout] property must NOT be a negative number!")
    private Integer requestTimeout;
    /**
     * Max retry count after calling failure
     */
    @Min(value = 0, message = "The [retryCount] property must NOT be a negative number!")
    @Max(value = 10, message = "The [retryCount] property must NOT be bigger than 10!")
    private Integer retryCount;
    /**
     * Max request or response message data payload size in bytes
     * NOT supported configuration per consumer or provider level
     */
    @Min(value = 0, message = "The [maxPayload] property of must NOT be a positive number!")
    private Integer maxPayload;
}
