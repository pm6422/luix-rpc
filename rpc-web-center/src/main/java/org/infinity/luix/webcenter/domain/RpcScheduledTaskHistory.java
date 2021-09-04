package org.infinity.luix.webcenter.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.time.Instant;

/**
 * Spring Data MongoDB collection for the RpcScheduledTaskHistory entity.
 */
@Data
@NoArgsConstructor
public class RpcScheduledTaskHistory implements Serializable {
    private static final long     serialVersionUID = 564976464702808036L;
    @Id
    private              String   id;
    /**
     * Task name
     */
    @Indexed
    private              String   name;
    /**
     * Registry identity
     */
    @NotEmpty
    private              String   registryIdentity;
    /**
     * Interface name
     * interfaceName or providerUrl must have value
     */
    private              String   interfaceName;
    /**
     * Form
     */
    private              String   form;
    /**
     * Version
     */
    private              String   version;
    /**
     * Method name. e.g, save
     */
    @NotEmpty
    private              String   methodName;
    /**
     * Method parameter list. e.g, ["org.infinity.rpc.democommon.domain.Authority"]
     */
    private              String[] methodParamTypes;
    /**
     * Method signature. e.g, invoke(java.util.List,java.lang.Long)
     */
    private              String   methodSignature;
    /**
     * Method arguments JSON string
     */
    private              String   argumentsJson;
    /**
     * Indicates whether it use cron expression, or fixed interval
     */
    private              Boolean  useCronExpression;
    /**
     * Cron expression
     */
    private              String   cronExpression;
    /**
     * Fixed rate interval
     */
    private              Long     fixedInterval;
    /**
     * Time unit of fixed rate interval, e.g. MINUTES, HOURS, DAYS
     */
    private              String   fixedIntervalUnit;
    /**
     * Execution time in milliseconds
     */
    private              Long     elapsed;
    /**
     * Indicates whether execute task successfully ot not
     */
    private              Boolean  success;
    /**
     * Failure reason
     */
    private              String   reason;
    /**
     * Created time
     */
    @CreatedDate
    private              Instant  createdTime;
    /**
     * Delete records at a specific time automatically by mongoDB
     */
    @Indexed(expireAfterSeconds = 0)
    private              Instant  expiryTime;
}