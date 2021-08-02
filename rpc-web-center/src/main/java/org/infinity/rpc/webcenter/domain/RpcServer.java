package org.infinity.rpc.webcenter.domain;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.infinity.rpc.core.server.buildin.ServerInfo;
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
@EqualsAndHashCode(callSuper = true)
public class RpcServer extends ServerInfo implements Serializable {
    private static final long   serialVersionUID        = 1L;
    public static final  String FIELD_REGISTRY_IDENTITY = "registryIdentity";
    public static final  String FIELD_ADDRESS           = "address";

    @Id
    private String  id;
    private String  registryIdentity;
    private String  address;
    private boolean active;
    @Transient
    private boolean providing;
    @Transient
    private boolean consuming;
}
