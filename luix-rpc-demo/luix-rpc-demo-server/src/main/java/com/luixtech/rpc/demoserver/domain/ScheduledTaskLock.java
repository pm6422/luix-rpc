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
 * Spring Data MongoDB collection for the ScheduledTaskLock entity.
 */
@Document
@Data
@NoArgsConstructor
public class ScheduledTaskLock implements Serializable {
    private static final long    serialVersionUID = -3024974222557800398L;
    @Id
    private              String  id;
    /**
     * Task name
     */
    @Indexed(unique = true)
    private              String  name;
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