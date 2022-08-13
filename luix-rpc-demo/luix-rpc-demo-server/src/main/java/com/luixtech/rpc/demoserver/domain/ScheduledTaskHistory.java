package com.luixtech.rpc.demoserver.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

/**
 * Spring Data MongoDB collection for the ScheduledTaskHistory entity.
 */
@Document
@Data
@NoArgsConstructor
public class ScheduledTaskHistory implements Serializable {
    private static final long    serialVersionUID = 564976464702808036L;
    @Id
    private              String  id;
    /**
     * Task name
     */
    @Indexed
    private              String  name;
    /**
     * Spring bean name
     */
    private              String  beanName;
    /**
     * Method arguments JSON string
     */
    private              String  argumentsJson;
    /**
     * Indicates whether it use cron expression, or fixed interval
     */
    private              Boolean useCronExpression;
    /**
     * Cron expression
     */
    private              String  cronExpression;
    /**
     * Fixed rate interval
     */
    private              Long    fixedInterval;
    /**
     * Time unit of fixed rate interval, e.g. MINUTES, HOURS, DAYS
     */
    private              String  fixedIntervalUnit;
    /**
     * Execution time in milliseconds
     */
    private              Long    elapsed;
    /**
     * Indicates whether execute task successfully ot not
     */
    private              Boolean success;
    /**
     * Failure reason
     */
    private              String  reason;
    /**
     * Created time
     */
    @CreatedDate
    private              Instant createdTime;
    /**
     * Delete records at a specific time automatically by mongoDB
     */
    @Indexed(expireAfterSeconds = 0)
    private              Instant expiryTime;
}