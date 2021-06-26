package org.infinity.rpc.webcenter.domain;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

/**
 * Spring Data MongoDB collection for the RpcServer entity.
 */
@ApiModel("RPC server")
@Document(collection = "RpcServer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcServer implements Serializable {
    private static final long   serialVersionUID        = 1L;
    public static final  String FIELD_REGISTRY_IDENTITY = "registryIdentity";
    public static final  String FIELD_INTERFACE_NAME    = "address";
    public static final  String FIELD_PROVIDING         = "providing";
    public static final  String FIELD_CONSUMING         = "consuming";

    @Id
    private String  id;
    private String  registryIdentity;
    private String  address;
    private Boolean active;
    @Transient
    private Boolean providing;
    @Transient
    private Boolean consuming;
}
