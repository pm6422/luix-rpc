package org.infinity.rpc.webcenter.domain;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.infinity.rpc.core.server.stub.ProviderStub;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.core.utils.name.ProviderStubBeanNameBuilder;
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

    public static RpcProvider of(Url providerUrl, Url registryUrl) {
        RpcProvider rpcProvider = new RpcProvider();
        // Set ID with identity
        String id = ProviderStubBeanNameBuilder
                .builder(providerUrl.getPath())
                .disablePrefix()
                .form(providerUrl.getForm())
                .version(providerUrl.getVersion())
                .build();
        rpcProvider.setId(id);
        rpcProvider.setInterfaceName(providerUrl.getPath());
        rpcProvider.setForm(providerUrl.getForm());
        rpcProvider.setVersion(providerUrl.getVersion());
        rpcProvider.setApplication(providerUrl.getOption(APP));
        rpcProvider.setHost(providerUrl.getHost());
        rpcProvider.setAddress(providerUrl.getAddress());
        rpcProvider.setProviderUrl(providerUrl.toFullStr());
        rpcProvider.setRegistryIdentity(registryUrl.getIdentity());
        rpcProvider.setActive(true);
        return rpcProvider;
    }
}
