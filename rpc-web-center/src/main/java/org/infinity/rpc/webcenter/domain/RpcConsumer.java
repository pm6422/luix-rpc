package org.infinity.rpc.webcenter.domain;

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
@Document(collection = "RpcConsumer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RpcConsumer implements Serializable {
    private static final long   serialVersionUID        = 1L;
    public static final  String FIELD_REGISTRY_IDENTITY = "registryIdentity";
    public static final  String FIELD_INTERFACE_NAME    = "interfaceName";
    public static final  String FIELD_APPLICATION       = "application";
    public static final  String FIELD_ADDRESS           = "address";
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
    private Instant createdTime;
    private Instant modifiedTime;

    public static RpcConsumer of(Url consumerUrl, Url registryUrl) {
        RpcConsumer consumer = new RpcConsumer();
        consumer.setId(registryUrl.getIdentity() + ":" + consumerUrl.getIdentity());
        consumer.setInterfaceName(consumerUrl.getPath());
        consumer.setForm(consumerUrl.getForm());
        consumer.setVersion(consumerUrl.getVersion());
        consumer.setApplication(consumerUrl.getOption(APP));
        consumer.setAddress(consumerUrl.getAddress());
        consumer.setUrl(consumerUrl.toFullStr());
        consumer.setRegistryIdentity(registryUrl.getIdentity());
        consumer.setActive(true);
        consumer.setModifiedTime(Instant.now());
        return consumer;
    }
}
