package org.infinity.luix.webcenter.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.infinity.luix.core.url.Url;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

import static org.infinity.luix.core.constant.ApplicationConstants.APP;

/**
 * Spring Data MongoDB collection for the RpcProvider entity.
 */
@Document(collection = "RpcProvider")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcProvider implements Serializable {
    private static final long   serialVersionUID        = 1L;
    public static final  String FIELD_REGISTRY_IDENTITY = "registryIdentity";
    public static final  String FIELD_INTERFACE_NAME    = "interfaceName";
    public static final  String FIELD_APPLICATION       = "application";
    public static final  String FIELD_ADDRESS           = "address";
    public static final  String FIELD_URL               = "url";
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
    private boolean active;
    private boolean consuming;
    private Instant modifiedTime;

    public static RpcProvider of(Url providerUrl, Url registryUrl) {
        RpcProvider rpcProvider = new RpcProvider();
        String id = RpcService.generateMd5Id(providerUrl.getIdentity(), registryUrl.getIdentity());
        rpcProvider.setId(id);
        rpcProvider.setRegistryIdentity(registryUrl.getIdentity());
        rpcProvider.setInterfaceName(providerUrl.getPath());
        rpcProvider.setForm(providerUrl.getForm());
        rpcProvider.setVersion(providerUrl.getVersion());
        rpcProvider.setApplication(providerUrl.getOption(APP));
        rpcProvider.setAddress(providerUrl.getAddress());
        rpcProvider.setUrl(providerUrl.toFullStr());
        rpcProvider.setActive(true);
        rpcProvider.setModifiedTime(Instant.now());
        return rpcProvider;
    }
}
