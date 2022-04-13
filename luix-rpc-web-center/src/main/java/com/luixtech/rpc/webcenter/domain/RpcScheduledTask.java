package com.luixtech.rpc.webcenter.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.luixtech.rpc.webcenter.domain.base.AbstractAuditableDomain;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Data MongoDB collection for the RpcScheduledTask entity.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RpcScheduledTask extends AbstractAuditableDomain implements Serializable {
    private static final long         serialVersionUID              = 8878535528271740314L;
    public static final  String       UNIT_SECONDS                  = "SECONDS";
    public static final  String       UNIT_MINUTES                  = "MINUTES";
    public static final  String       UNIT_HOURS                    = "HOURS";
    public static final  String       UNIT_DAYS                     = "DAYS";
    public static final  List<String> AVAILABLE_FIXED_INTERVAL_UNIT = Arrays.asList(UNIT_SECONDS, UNIT_MINUTES, UNIT_HOURS, UNIT_DAYS);
    public static final  List<String> AVAILABLE_INITIAL_DELAY_UNIT  = Arrays.asList(UNIT_SECONDS, UNIT_MINUTES, UNIT_HOURS);
    public static final  String       FIELD_REGISTRY_IDENTITY       = "registryIdentity";
    public static final  String       FIELD_NAME                    = "name";
    public static final  String       FIELD_INTERFACE_NAME          = "interfaceName";
    public static final  String       FIELD_FORM                    = "form";
    public static final  String       FIELD_VERSION                 = "version";
    public static final  String       FIELD_METHOD_NAME             = "methodName";
    public static final  String       FIELD_METHOD_SIGNATURE        = "methodSignature";

    /**
     * Task name
     */
    @Indexed(unique = true)
    private String   name;
    /**
     * Registry identity
     */
    @NotEmpty
    private String   registryIdentity;
    /**
     * Interface name
     * interfaceName or providerUrl must have value
     */
    private String   interfaceName;
    /**
     * Form
     */
    private String   form;
    /**
     * Version
     */
    private String   version;
    /**
     * Retry count
     */
    private Integer  retryCount;
    /**
     * Request timeout in milliseconds
     */
    private Integer  requestTimeout;
    /**
     * Fault tolerance
     */
    private String   faultTolerance;
    /**
     * Method name. e.g, save
     */
    @NotEmpty
    private String   methodName;
    /**
     * Method parameter list. e.g, ["com.luixtech.rpc.democommon.domain.Authority"]
     */
    private String[] methodParamTypes;
    /**
     * Method signature. e.g, invoke(java.util.List,java.lang.Long)
     */
    private String   methodSignature;
    /**
     * Method arguments JSON string
     */
    private String   argumentsJson;
    /**
     * Indicates whether it use cron expression, or fixed interval
     */
    private Boolean  useCronExpression;
    /**
     * Cron expression
     * https://cron.qqe2.com
     */
    private String   cronExpression;
    /**
     * Fixed rate interval
     */
    @Positive
    private Long     fixedInterval;
    /**
     * Time unit of fixed rate interval, e.g. MINUTES, HOURS, DAYS
     */
    private String   fixedIntervalUnit;
    /**
     * The time to delay first execution
     */
    @Positive
    private Long     initialDelay;
    /**
     * o
     * Time unit of initialDelay, e.g. SECONDS, MINUTES, HOURS
     */
    private String   initialDelayUnit;
    /**
     * Start time
     */
    private Instant  startTime;
    /**
     * Stop time
     */
    private Instant  stopTime;
    /**
     * Enabled
     */
    @NotNull
    private Boolean  enabled;
    /**
     * Remarks
     */
    private String   remark;
}