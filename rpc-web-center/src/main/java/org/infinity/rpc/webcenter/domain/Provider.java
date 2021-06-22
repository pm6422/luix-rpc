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
 * Spring Data MongoDB collection for the Provider entity.
 */
@ApiModel("Service provider")
@Document(collection = "Provider")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Provider implements Serializable {
    private static final long   serialVersionUID        = 1L;
    public static final  String FIELD_INTERFACE_NAME    = "interfaceName";
    public static final  String FIELD_APPLICATION       = "application";
    public static final  String FIELD_REGISTRY_IDENTITY = "registryIdentity";
    public static final  String FIELD_ACTIVE            = "active";

    @Id
    protected String  id;
    private   String  interfaceName;
    private   String  form;
    private   String  version;
    private   String  application;
    private   String  host;
    private   String  address;
    private   String  providerUrl;
    private   String  registryIdentity;
    private   Boolean active    = false;
    private   Boolean consuming = false;
    private   Instant createdTime;
    private   Instant modifiedTime;

    public static Provider of(Url providerUrl, Url registryUrl) {
        Provider provider = new Provider();
        // Set ID with identity
        provider.setId(providerUrl.getIdentity());
        provider.setInterfaceName(providerUrl.getPath());
        provider.setForm(providerUrl.getForm());
        provider.setVersion(providerUrl.getVersion());
        provider.setApplication(providerUrl.getOption(APP));
        provider.setHost(providerUrl.getHost());
        provider.setAddress(providerUrl.getAddress());
        provider.setProviderUrl(providerUrl.toFullStr());
        provider.setRegistryIdentity(registryUrl.getIdentity());
        provider.setActive(true);
        return provider;
    }
}
