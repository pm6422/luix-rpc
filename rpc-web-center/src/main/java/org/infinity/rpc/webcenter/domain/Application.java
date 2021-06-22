package org.infinity.rpc.webcenter.domain;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;

/**
 * Spring Data MongoDB collection for the App entity.
 */
@ApiModel("应用")
@Document(collection = "Application")
@Data
@NoArgsConstructor
public class Application implements Serializable {
    private static final long   serialVersionUID      = 1L;
    public static final  String FIELD_NAME            = "name";
    public static final  String FIELD_ACTIVE_PROVIDER = "activeProvider";
    public static final  String FIELD_ACTIVE_CONSUMER = "activeConsumer";

    @Id
    protected String  id;
    private   String  name;
    private   String  description;
    private   String  team;
    private   String  ownerMail;
    private   String  env;
    private   String  registryIdentity;
    private   String  jarVersion;
    private   Boolean activeProvider;
    private   Boolean activeConsumer;
    private   Instant createdTime;
    private   Instant modifiedTime;
}
