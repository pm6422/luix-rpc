package com.luixtech.rpc.demoserver.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.luixtech.rpc.democommon.domain.base.AbstractAuditableDomain;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Data MongoDB collection for the ScheduledTask entity.
 */
@Document(collection = "ScheduledTask")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ScheduledTask extends AbstractAuditableDomain implements Serializable {
    private static final long         serialVersionUID              = 8878535528271740314L;
    public static final  String       UNIT_MINUTES                  = "MINUTES";
    public static final  String       UNIT_HOURS                    = "HOURS";
    public static final  String       UNIT_DAYS                     = "DAYS";
    public static final  List<String> AVAILABLE_FIXED_INTERVAL_UNIT = Arrays.asList(UNIT_MINUTES, UNIT_HOURS, UNIT_DAYS);
    /**
     * Task name
     */
    @Indexed(unique = true)
    private              String       name;
    /**
     * Spring bean name
     */
    @NotEmpty
    @Indexed(unique = true)
    private              String       beanName;
    /**
     * Method arguments JSON string
     */
    private              String       argumentsJson;
    /**
     * Indicates whether it use cron expression, or fixed interval
     */
    private              Boolean      useCronExpression;
    /**
     * Cron expression
     * https://cron.qqe2.com
     */
    private              String       cronExpression;
    /**
     * Fixed rate interval
     */
    @Positive
    private              Long         fixedInterval;
    /**
     * Time unit of fixed rate interval, e.g. MINUTES, HOURS, DAYS
     */
    private              String       fixedIntervalUnit;
    /**
     * Start time
     */
    private              Instant      startTime;
    /**
     * Stop time
     */
    private              Instant      stopTime;
    /**
     * Enabled
     */
    @NotNull
    private              Boolean      enabled;
    /**
     * Remarks
     */
    private              String       remark;
}