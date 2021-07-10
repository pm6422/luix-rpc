package org.infinity.rpc.webcenter.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.infinity.rpc.democommon.domain.base.AbstractAuditableDomain;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the RpcTask entity.
 */
@Document(collection = "RpcTask")
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RpcTask extends AbstractAuditableDomain implements Serializable {
    private static final long    serialVersionUID = 8878535528271740314L;
    /**
     * Task name
     */
    @Indexed(unique = true)
    private              String  name;
    /**
     * Spring bean name
     */
    @NotEmpty
    @Indexed(unique = true)
    private              String  beanName;
    /**
     * Method arguments JSON string
     */
    private              String  argumentsJson;
    /**
     * Cron expression
     * https://cron.qqe2.com
     */
    @NotEmpty
    private              String  cronExpression;
    /**
     * Indicates whether execute task on all hosts or one host
     */
    private              boolean allHostsRun;
    /**
     * Remarks
     */
    private              String  remark;
    /**
     * Enabled
     */
    @NotNull
    private              Boolean enabled;
}