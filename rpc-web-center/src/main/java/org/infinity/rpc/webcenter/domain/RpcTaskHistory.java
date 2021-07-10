package org.infinity.rpc.webcenter.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.time.Instant;

/**
 * Spring Data MongoDB collection for the RpcTaskHistory entity.
 */
@Document(collection = "RpcTaskHistory")
@Data
@NoArgsConstructor
public class RpcTaskHistory implements Serializable {
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
     * Provider url
     * interfaceName or providerUrl must have value
     */
    private              String   providerUrl;
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
     * Method arguments JSON string
     */
    private              String   argumentsJson;
    /**
     * Cron expression
     */
    private              String   cronExpression;
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