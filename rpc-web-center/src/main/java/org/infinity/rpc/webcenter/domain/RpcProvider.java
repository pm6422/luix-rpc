package org.infinity.rpc.webcenter.domain;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.infinity.rpc.core.url.Url;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

import static org.infinity.rpc.core.constant.ApplicationConstants.APP;

/**
 * Spring Data MongoDB collection for the RpcProvider entity.
 */
@ApiModel("RPC service provider")
@Document(collection = "RpcProvider")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcProvider implements Serializable {
    private static final long   serialVersionUID        = 1L;
    public static final  String FIELD_REGISTRY_IDENTITY = "registryIdentity";
    public static final  String FIELD_INTERFACE_NAME    = "interfaceName";
    public static final  String FIELD_APPLICATION       = "application";
    public static final  String FIELD_ACTIVE            = "active";

    @Id
    private String  id;
    private String  registryIdentity;
    private String  interfaceName;
    private String  form;
    private String  version;
    private String  application;
    private String  address;
    private String  url;
    private Boolean active    = false;
    private Boolean consuming = false;
    private Instant createdTime;
    private Instant modifiedTime;

    public static RpcProvider of(Url providerUrl, Url registryUrl) {
        RpcProvider rpcProvider = new RpcProvider();
        rpcProvider.setId(registryUrl.getIdentity() + ":" + providerUrl.getIdentity());
        rpcProvider.setInterfaceName(providerUrl.getPath());
        rpcProvider.setForm(providerUrl.getForm());
        rpcProvider.setVersion(providerUrl.getVersion());
        rpcProvider.setApplication(providerUrl.getOption(APP));
        rpcProvider.setAddress(providerUrl.getAddress());
        rpcProvider.setUrl(providerUrl.toFullStr());
        rpcProvider.setRegistryIdentity(registryUrl.getIdentity());
        rpcProvider.setActive(true);
        return rpcProvider;
    }
}
