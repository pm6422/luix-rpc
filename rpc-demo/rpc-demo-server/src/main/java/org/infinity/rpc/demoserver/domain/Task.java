package org.infinity.rpc.demoserver.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.infinity.rpc.democommon.domain.base.AbstractAuditableDomain;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Data MongoDB collection for the Task entity.
 */
@Document(collection = "Task")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Task extends AbstractAuditableDomain implements Serializable {
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
     * Remarks
     */
    private              String       remark;
    /**
     * Enabled
     */
    @NotNull
    private              Boolean      enabled;
}