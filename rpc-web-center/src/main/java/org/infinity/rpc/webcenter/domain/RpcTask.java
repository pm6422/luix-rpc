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
    private static final long   serialVersionUID        = 8878535528271740314L;
    public static final  String FIELD_REGISTRY_IDENTITY = "registryIdentity";
    public static final  String FIELD_NAME              = "name";
    public static final  String FIELD_INTERFACE_NAME    = "interfaceName";
    public static final  String FIELD_FORM              = "form";
    public static final  String FIELD_VERSION           = "version";
    public static final  String FIELD_METHOD_NAME       = "methodName";

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
     * Method name. e.g, save
     */
    @NotEmpty
    private String   methodName;
    /**
     * Method parameter list. e.g, ["org.infinity.rpc.democommon.domain.Authority"]
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
     * Cron expression
     * https://cron.qqe2.com
     */
    @NotEmpty
    private String   cronExpression;
    /**
     * Indicates whether execute task on all hosts or one host
     */
    private boolean  allHostsRun;
    /**
     * Remarks
     */
    private String   remark;
    /**
     * Enabled
     */
    @NotNull
    private Boolean  enabled;
}