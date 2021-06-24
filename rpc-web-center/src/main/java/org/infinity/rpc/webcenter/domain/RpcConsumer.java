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
 * Spring Data MongoDB collection for the RpcConsumer entity.
 */
@ApiModel("RPC service consumer")
@Document(collection = "RpcConsumer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcConsumer implements Serializable {
    private static final long   serialVersionUID        = 1L;
    public static final  String FIELD_INTERFACE_NAME    = "interfaceName";
    public static final  String FIELD_APPLICATION       = "application";
    public static final  String FIELD_REGISTRY_IDENTITY = "registryIdentity";
    public static final  String FIELD_ACTIVE            = "active";

    @Id
    private String  id;
    private String  interfaceName;
    private String  form;
    private String  version;
    private String  application;
    private String  host;
    private String  address;
    private String  consumerUrl;
    private String  registryIdentity;
    private Boolean active = false;
    private Instant createdTime;
    private Instant modifiedTime;

    public static RpcConsumer of(Url consumerUrl, Url registryUrl) {
        RpcConsumer provider = new RpcConsumer();
        provider.setId(registryUrl.getIdentity() + ":" + consumerUrl.getIdentity());
        provider.setInterfaceName(consumerUrl.getPath());
        provider.setForm(consumerUrl.getForm());
        provider.setVersion(consumerUrl.getVersion());
        provider.setApplication(consumerUrl.getOption(APP));
        provider.setHost(consumerUrl.getHost());
        provider.setAddress(consumerUrl.getAddress());
        provider.setConsumerUrl(consumerUrl.toFullStr());
        provider.setRegistryIdentity(registryUrl.getIdentity());
        provider.setActive(true);
        return provider;
    }
}
