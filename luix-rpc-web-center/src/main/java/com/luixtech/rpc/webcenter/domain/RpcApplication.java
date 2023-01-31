package com.luixtech.rpc.webcenter.domain;

import com.luixtech.rpc.core.config.impl.ApplicationConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

/**
 * Spring Data MongoDB collection for the RpcApplication entity.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Document
@Data
@NoArgsConstructor
public class RpcApplication extends ApplicationConfig implements Serializable {
    private static final long   serialVersionUID = 1L;
    public static final  String FIELD_NAME       = "name";
    public static final  String FIELD_ACTIVE     = "active";
    public static final  String FIELD_PROVIDING  = "providing";
    public static final  String FIELD_CONSUMING  = "consuming";

    @Id
    private String  tid;
    private String  registryIdentity;
    private boolean active;
    @Transient
    private boolean providing;
    @Transient
    private boolean consuming;
    private Instant createdTime;
    private Instant modifiedTime;
}
